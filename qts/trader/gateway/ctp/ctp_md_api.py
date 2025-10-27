from datetime import datetime
from ..base_gateway import BaseGateway
from .base import *

class CtpMdApi(mdapi.CThostFtdcMdSpi):
    """
    CTP行情接口
    """

    def __init__(self, gateway: BaseGateway) -> None:
        """构造函数"""
        super().__init__()

        self.gateway: BaseGateway = gateway
        self.acct_conf: AcctConf= gateway.acct_detail.acct_info.conf
        self.name: str = gateway.gateway_name

        self.reqid: int = 0

        self.connect_status: bool = False
        self.login_status: bool = False
        self.subscribed: set[str] = set()

        __,self.address = self.acct_conf.md_addr.split('|')
        self.current_date: str = datetime.now().strftime("%Y%m%d")
        self.api: mdapi.CThostFtdcMdApi = None

        log.info(f"初始化行情接口,名称:{self.name}, 地址:{self.address}")

    @property
    def md_status(self):
        return self.login_status

    def OnFrontConnected(self) -> None:
        """服务器连接成功回报"""
        log.info("行情服务器连接成功")
        self.login()

    def OnFrontDisconnected(self, reason: int) -> None:
        """服务器连接断开回报"""
        self.login_status = False
        self.gateway.on_status(StatusData(type="md",status=False))
        log.info(f"行情服务器连接断开，原因{reason}")

    def OnRspUserLogin(self, pRspUserLogin: mdapi.CThostFtdcRspUserLoginField, pRspInfo: mdapi.CThostFtdcRspInfoField,
                       nRequestID: int, bIsLast: bool) -> "void":
        """用户登录请求回报"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"行情服务器登录失败,{pRspInfo.ErrorMsg}")
        else:
            self.login_status = True
            self.gateway.on_status(StatusData(type="md",status=True))
            self.trade_date = pRspUserLogin.TradingDay
            log.info(f"行情服务器登录成功,交易日:{pRspUserLogin.TradingDay}", )

            #订阅合约
            self.api.SubscribeMarketData([symbol.encode('utf-8') for symbol in self.subscribed], len(self.subscribed))

    def OnRspError(self, error: dict, reqid: int, last: bool) -> None:
        """请求报错回报"""
        log.error(f"行情接口报错 {error}")

    def OnRspSubMarketData(self, pSpecificInstrument: mdapi.CThostFtdcSpecificInstrumentField,
                           pRspInfo: mdapi.CThostFtdcRspInfoField, nRequestID: int, bIsLast: bool) -> "void":
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            print(f"行情订阅失败  [{pSpecificInstrument.InstrumentID}] {pRspInfo.ErrorMsg}")
            return
        if pSpecificInstrument != None:
            log.info(f"行情订阅成功 {pSpecificInstrument.InstrumentID}")

    def OnRtnDepthMarketData(self, data: mdapi.CThostFtdcDepthMarketDataField) -> None:
        """行情数据推送"""
        # 过滤没有时间戳的异常行情数据
        if not data.UpdateTime:
            return

        # 过滤还没有收到合约数据前的行情推送
        symbol: str = data.InstrumentID
        contract: ContractData = self.gateway.get_contract(symbol)
        if not contract:
            return

        # 对大商所的交易日字段取本地日期
        if not data.ActionDay or contract.exchange == Exchange.DCE:
            date_str: str = self.current_date
        else:
            date_str: str = data.ActionDay

        timestamp: str = f"{date_str} {data.UpdateTime}.{data.UpdateMillisec}"
        dt: datetime = datetime.strptime(timestamp, "%Y%m%d %H:%M:%S.%f")
        dt: datetime = dt.replace(tzinfo=CHINA_TZ)

        tick: TickData = TickData(
            symbol=symbol,
            exchange=contract.exchange,
            time=dt,
            name=contract.name,
            volume=data.Volume,
            turnover=data.Turnover,
            open_interest=data.OpenInterest,
            last_price=adjust_price(data.LastPrice),
            limit_up=data.UpperLimitPrice,
            limit_down=data.LowerLimitPrice,
            open_price=adjust_price(data.OpenPrice),
            high_price=adjust_price(data.HighestPrice),
            low_price=adjust_price(data.LowestPrice),
            pre_close=adjust_price(data.PreClosePrice),
            bid_price_1=adjust_price(data.BidPrice1),
            ask_price_1=adjust_price(data.AskPrice1),
            bid_volume_1=data.BidVolume1,
            ask_volume_1=data.AskVolume1,
        )

        if data.BidVolume2 or data.AskVolume2:
            tick.bid_price_2 = adjust_price(data.BidPrice2)
            tick.bid_price_3 = adjust_price(data.BidPrice3)
            tick.bid_price_4 = adjust_price(data.BidPrice4)
            tick.bid_price_5 = adjust_price(data.BidPrice5)

            tick.ask_price_2 = adjust_price(data.AskPrice2)
            tick.ask_price_3 = adjust_price(data.AskPrice3)
            tick.ask_price_4 = adjust_price(data.AskPrice4)
            tick.ask_price_5 = adjust_price(data.AskPrice5)

            tick.bid_volume_2 = data.BidVolume2
            tick.bid_volume_3 = data.BidVolume3
            tick.bid_volume_4 = data.BidVolume4
            tick.bid_volume_5 = data.BidVolume5

            tick.ask_volume_2 = data.AskVolume2
            tick.ask_volume_3 = data.AskVolume3
            tick.ask_volume_4 = data.AskVolume4
            tick.ask_volume_5 = data.AskVolume5

        self.gateway.on_tick(tick)

    def connect(self) -> None:
        """连接服务器"""
        # 禁止重复发起连接，会导致异常崩溃
        if not self.connect_status:
            log.info("开始连接行情服务器...")
            path = get_data_path(f"tmp/{self.name.lower()}")
            flow_path = (str(path) + "/md-")
            self.api = mdapi.CThostFtdcMdApi.CreateFtdcMdApi(flow_path)
            self.api.RegisterFront(self.address)
            self.api.RegisterSpi(self)
            self.api.Init()
            self.connect_status = True
    
    def close(self) -> None:
        """关闭连接"""
        if self.connect_status:
            self.api.Release()
            self.api = None
            self.connect_status = False
        self.login_status = False
        self.gateway.on_status(StatusData(type="md",status=False))
        log.info("行情接口关闭")

    def login(self) -> None:
        """用户登录"""
        self.reqid += 1
        req: mdapi.CThostFtdcReqUserLoginField = mdapi.CThostFtdcReqUserLoginField()
        self.api.ReqUserLogin(req, self.reqid)

    def subscribe(self, req: SubscribeRequest) -> None:
        """订阅行情"""
        if self.login_status:
            unSubs = [symbol for symbol in req.symbols if symbol not in self.subscribed]
            self.api.SubscribeMarketData([symbol.encode('utf-8') for symbol in unSubs], len(unSubs))
            self.subscribed.update(unSubs)

    def update_date(self) -> None:
        """更新当前日期"""
        self.current_date = datetime.now().strftime("%Y%m%d")