import os
import queue
import threading
from datetime import datetime
from pathlib import Path
from time import sleep
from typing import Dict

from config import get_setting
from qts.log import get_logger

from ..base_gateway import BaseGateway
from model.object import OrderRequest, CancelRequest, OrderData, Position, AccountData, TradeData,StatusData
from .lib import *

log = get_logger(__name__)


class CtpTdApi(CThostFtdcTraderSpi):
    """"""

    def __init__(self, gateway: BaseGateway) -> None:
        """构造函数"""
        super().__init__()

        self.gateway: BaseGateway = gateway
        self.acct_info: AcctInfo = gateway.acct_info
        self.gateway_name: str = gateway.gateway_name

        __,self.address= self.acct_info.conf.td_addr.split('|')
        self.user,self.pwd = self.acct_info.conf.user.split('|')
        self.broker = self.acct_info.conf.broker
        self.auth = self.acct_info.conf.auth

        self.reqid: int = 0
        self.order_ref: int = 0
        self.connect_status: bool = False
        self.login_status: bool = False
        self.contract_inited: bool = False
        self.frontid: int = 0
        self.sessionid: int = 0
        self.order_data: list[dict] = []
        self.trade_data: list[dict] = []
        self.positions: dict[str, Position] = {}
        self.sysid_orderid_map: dict[str, str] = {}

        self.api: CThostFtdcTraderApi = None

        self.semaphore = threading.Semaphore(1)

    @property
    def td_status(self):
        return self.login_status

    def OnFrontConnected(self) -> None:
        """服务器连接成功回报"""
        log.info("交易服务器连接成功")

        if self.auth:
            self.authenticate()
        else:
            self.login()

    def OnFrontDisconnected(self, reason: int) -> None:
        """服务器连接断开回报"""
        self.login_status = False
        self.gateway.on_status(StatusData(type="td",status=False))
        log.error(f"交易服务器连接断开，原因{reason}")

    def OnRspAuthenticate(self, pRspAuthenticateField: "CThostFtdcRspAuthenticateField",
                          pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """用户授权验证回报"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"交易服务器授权验证失败,{pRspInfo.ErrorMsg}")
            return

        log.info("交易服务器授权验证成功")
        self.login()

    def OnRspUserLogin(self, pRspUserLogin: "CThostFtdcRspUserLoginField", pRspInfo: "CThostFtdcRspInfoField",
                       nRequestID: "int", bIsLast: "bool") -> "void":
        """用户登录请求回报"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"交易服务器登录失败,{pRspInfo.ErrorMsg}")
            return

        self.frontid = pRspUserLogin.FrontID
        self.sessionid = pRspUserLogin.SessionID
        self.login_status = True
        self.trading_day = pRspUserLogin.TradingDay

        self.gateway.on_status(StatusData(type="td",status=True))
        self.acct_info.trading_day = self.trading_day
        log.info(f"交易服务器登录成功 {pRspUserLogin.TradingDay}")

        # 异步发起查询
        threading.Thread(target=self.__run_query).start()

    def OnRspOrderInsert(self, data: dict, error: dict, reqid: int, last: bool) -> None:
        """委托下单失败回报"""
        order_ref: str = data["OrderRef"]
        orderid: str = f"{self.frontid}_{self.sessionid}_{order_ref}"

        symbol: str = data["InstrumentID"]
        contract: ContractData = self.gateway.contracts_map[symbol]

        order: OrderData = OrderData(
            symbol=symbol,
            exchange=contract.exchange,
            orderid=orderid,
            direction=DIRECTION_CTP2VT[data["Direction"]],
            offset=OFFSET_CTP2VT.get(data["CombOffsetFlag"], Offset.NONE),
            price=data["LimitPrice"],
            volume=data["VolumeTotalOriginal"],
            status=Status.REJECTED
        )
        self.gateway.on_order(order)

        self.gateway.write_error("交易委托失败", error)

    def OnRspOrderAction(self, data: dict, error: dict, reqid: int, last: bool) -> None:
        """委托撤单失败回报"""
        self.gateway.write_error("交易撤单失败", error)

    def OnRspSettlementInfoConfirm(self, pSettlementInfoConfirm: "CThostFtdcSettlementInfoConfirmField",
                                   pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """确认结算单回报"""
        log.info("结算信息确认成功")
        self.semaphore.release()

    def OnRspQryInvestorPosition(self, pInvestorPosition: "CThostFtdcInvestorPositionField",
                                 pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """持仓查询回报"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"查询持仓失败,{pRspInfo.ErrorMsg}")
            self.semaphore.release()
            return
        if pInvestorPosition:
            # 必须已经收到了合约信息后才能处理
            symbol: str = pInvestorPosition.InstrumentID
            contract: ContractData = self.gateway.contracts_map.get(symbol, None)

            if contract:
                # 获取之前缓存的持仓数据缓存
                key: str = f"{pInvestorPosition.InstrumentID, pInvestorPosition.PosiDirection}"
                position: Position = self.positions.get(key, None)
                if not position:
                    position = Position(
                        id = key,
                        symbol=pInvestorPosition.InstrumentID,
                        exchange=contract.exchange,
                        direction=DIRECTION_CTP2VT[pInvestorPosition.PosiDirection],
                    )
                    self.positions[key] = position

                # 获取合约的乘数信息
                size: int = contract.multiple

                volume: int = pInvestorPosition.Position
                td_volume: int = pInvestorPosition.TodayPosition if pInvestorPosition.TodayPosition else 0
                yd_volume: int = volume - td_volume
                
                position.volume += volume
                position.td_volume += td_volume
                position.yd_volume += yd_volume
                position.yestday_volume += pInvestorPosition.YdPosition

                # 计算持仓成本/均价
                position.hold_cost +=pInvestorPosition.PositionCost
                position.price = round(position.hold_cost / (position.volume * size),2)
                position.hold_profit += round(pInvestorPosition.PositionProfit,2)
                position.close_profit += round(pInvestorPosition.CloseProfit,2)

                position.pre_price = pInvestorPosition.PreSettlementPrice
                position.margin += round(pInvestorPosition.UseMargin,2)
                position.commission += round(pInvestorPosition.Commission,2)
                # 更新仓位冻结数量
                if position.direction == Direction.LONG:
                    position.frozen += pInvestorPosition.ShortFrozen
                else:
                    position.frozen += pInvestorPosition.LongFrozen
                position.available = position.volume - position.frozen
                

        if bIsLast:
            self.gateway.on_positions(list(self.positions.values()))
            log.info(f"持仓信息查询成功,共{len(self.positions)}条")
            self.positions.clear()
            self.semaphore.release()

    def OnRspQryTradingAccount(self, pTradingAccount: "CThostFtdcTradingAccountField",
                               pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """资金查询回报"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"查询资金失败,{pRspInfo.ErrorMsg}")
            self.semaphore.release()
            return
        account: AccountData = AccountData(
            accountid=pTradingAccount.AccountID,
            balance=round(pTradingAccount.Balance,2),
            frozen=round(pTradingAccount.FrozenMargin + pTradingAccount.FrozenCash + pTradingAccount.FrozenCommission,2),
        )
        account.available = round(pTradingAccount.Available,2)
        account.margin_rate = round(pTradingAccount.CurrMargin/pTradingAccount.Balance,4)
        account.hold_profit = round(pTradingAccount.PositionProfit,2)
        account.close_profit = round(pTradingAccount.CloseProfit,2)
        self.gateway.on_account(account)
        log.info(f"查询资金成功,可用资金{account.available},持仓资金{account.balance},冻结资金{account.frozen},保证金率{account.margin_rate},持仓盈亏{account.hold_profit},平仓盈亏{account.close_profit}")
        self.semaphore.release()

    def OnRspQryProduct(self, pProduct: "CThostFtdcProductField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int",
                        bIsLast: "bool") -> "void":
        """产品查询回报"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"查询产品失败,{pRspInfo.ErrorMsg}")
            self.semaphore.release()
            return

        product: Product = PRODUCT_CTP2VT.get(pProduct.ProductClass, None)
        if product:
            self.gateway.on_product(product)

        pass

    def OnRspQryInstrument(self, pInstrument: "CThostFtdcInstrumentField", pRspInfo: "CThostFtdcRspInfoField",
                           nRequestID: "int", bIsLast: "bool") -> "void":
        """合约查询回报"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"查询合约失败,{pRspInfo.ErrorMsg}")
            self.semaphore.release()
            return

        product: Product = PRODUCT_CTP2VT.get(pInstrument.ProductClass, None)
        if product:
            contract: ContractData = ContractData(
                symbol=pInstrument.InstrumentID,
                exchange=EXCHANGE_CTP2VT[pInstrument.ExchangeID],
                name=pInstrument.InstrumentName,
                product=product,
                multiple=pInstrument.VolumeMultiple,
                pricetick=pInstrument.PriceTick,
            )

            # 期权相关
            if contract.product == Product.OPTION:
                # 移除郑商所期权产品名称带有的C/P后缀
                if contract.exchange == Exchange.CZCE:
                    contract.option_portfolio = pInstrument.ProductID[:-1]
                else:
                    contract.option_portfolio = pInstrument.ProductID

                contract.option_underlying = pInstrument.UnderlyingInstrID
                contract.option_type = OPTIONTYPE_CTP2VT.get(pInstrument.OptionsType, None)
                contract.option_strike = pInstrument.StrikePrice
                contract.option_index = str(pInstrument.StrikePrice)
                contract.option_listed = datetime.strptime(pInstrument.OpenDate, "%Y%m%d")
                contract.option_expiry = datetime.strptime(pInstrument.ExpireDate, "%Y%m%d")

            # self.gateway.on_contract(contract)()
            if len(contract.symbol) <= 10:
                self.gateway.contracts_map[contract.symbol] = contract

        if bIsLast:
            self.contract_inited = True
            log.info(f"合约信息查询成功,共{len(self.gateway.contracts_map)}条")
            self.gateway.on_contract(self.gateway.contracts_map)
            self.semaphore.release()

    def OnRspQryTrade(self, pTrade: "CThostFtdcTradeField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """请求查询成交响应"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"查询成交失败,{pRspInfo.ErrorMsg}")
            self.semaphore.release()
            return

        if pTrade:
            symbol: str = pTrade.InstrumentID
            contract: ContractData = self.gateway.contracts_map[symbol]

            frontid: int = pTrade.FrontID
            sessionid: int = pTrade.SessionID
            order_ref: str = pTrade.OrderRef
            orderid: str = f"{frontid}_{sessionid}_{order_ref}"

            timestamp: str = f"{pTrade.TradeDate} {pTrade.TradeTime}"
            dt: datetime = datetime.strptime(timestamp, "%Y%m%d %H:%M:%S")
            dt: datetime = dt.replace(tzinfo=CHINA_TZ)

            trade: TradeData = TradeData(
                id=pTrade.TradeID,
                symbol=symbol,
                exchange=contract.exchange,
                order_ref=orderid,
                direction=DIRECTION_CTP2VT[pTrade.Direction],
                offset=OFFSET_CTP2VT[pTrade.OffsetFlag],
                price=pTrade.Price,
                volume=pTrade.Volume,
                time=dt            
                )
            self.gateway.on_trade(trade)

        if bIsLast:
            self.semaphore.release()
        pass

    def OnRspQryOrder(self, pOrder: "CThostFtdcOrderField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """请求查询报单响应"""
        pass

    def OnRtnOrder(self, data: dict) -> None:
        """委托更新推送"""
        if not self.contract_inited:
            self.order_data.append(data)
            return

        symbol: str = data["InstrumentID"]
        contract: ContractData = self.gateway.contracts_map[symbol]

        frontid: int = data["FrontID"]
        sessionid: int = data["SessionID"]
        order_ref: str = data["OrderRef"]
        orderid: str = f"{frontid}_{sessionid}_{order_ref}"

        status: Status = STATUS_CTP2VT.get(data["OrderStatus"], None)
        if not status:
            self.gateway.write_log(f"收到不支持的委托状态，委托号：{orderid}")
            return

        timestamp: str = f"{data['InsertDate']} {data['InsertTime']}"
        dt: datetime = datetime.strptime(timestamp, "%Y%m%d %H:%M:%S")
        dt: datetime = dt.replace(tzinfo=CHINA_TZ)

        tp: tuple = (data["OrderPriceType"], data["TimeCondition"], data["VolumeCondition"])
        order_type: OrderType = ORDERTYPE_CTP2VT.get(tp, None)
        if not order_type:
            self.gateway.write_log(f"收到不支持的委托类型，委托号：{orderid}")
            return

        order: OrderData = OrderData(
            symbol=symbol,
            exchange=contract.exchange,
            orderid=orderid,
            type=order_type,
            direction=DIRECTION_CTP2VT[data["Direction"]],
            offset=OFFSET_CTP2VT[data["CombOffsetFlag"]],
            price=data["LimitPrice"],
            volume=data["VolumeTotalOriginal"],
            traded=data["VolumeTraded"],
            status=status,
            datetime=dt,
            gateway_name=self.gateway_name
        )
        self.gateway.on_order(order)

        self.sysid_orderid_map[data["OrderSysID"]] = orderid

    def OnRtnTrade(self, data: dict) -> None:
        """成交数据推送"""
        if not self.contract_inited:
            self.trade_data.append(data)
            return

        symbol: str = data["InstrumentID"]
        contract: ContractData = self.gateway.contracts_map[symbol]

        orderid: str = self.sysid_orderid_map[data["OrderSysID"]]

        timestamp: str = f"{data['TradeDate']} {data['TradeTime']}"
        dt: datetime = datetime.strptime(timestamp, "%Y%m%d %H:%M:%S")
        dt: datetime = dt.replace(tzinfo=CHINA_TZ)

        trade: TradeData = TradeData(
            id=data["TradeID"],
            symbol=symbol,
            exchange=contract.exchange,
            order_ref=orderid,
            direction=DIRECTION_CTP2VT[data["Direction"]],
            offset=OFFSET_CTP2VT[data["OffsetFlag"]],
            price=data["Price"],
            volume=data["Volume"],
            time=dt
        )
        self.gateway.on_trade(trade)

    def next_reqid(self):
        self.reqid += 1
        return self.reqid

    def connect(self) -> None:
        """连接服务器"""
        if not self.connect_status:
            log.info("开始连接交易服务器...")
            path = get_data_path(self.gateway_name.lower())
            flow_path = (str(path) + "\\Td")
            self.api = CThostFtdcTraderApi.CreateFtdcTraderApi(flow_path)
            self.api.RegisterSpi(self)
            self.api.RegisterFront(self.address)
            self.api.SubscribePrivateTopic(THOST_TERT_QUICK)
            self.api.SubscribePublicTopic(THOST_TERT_QUICK)
            self.api.Init()
            self.connect_status = True

    def close(self) -> None:
        """关闭连接"""
        if self.connect_status:
            self.api.Release()
            self.api = None
            self.connect_status = False
        self.login_status = False
        self.gateway.on_status(StatusData(type="td",status=False))
        log.info("ctp td api closed")

    def authenticate(self) -> None:
        """发起授权验证"""

        appid, auth_code = self.auth.split(':')
        req = CThostFtdcReqAuthenticateField()
        req.BrokerID = self.broker
        req.UserID = self.user
        req.AppID = appid
        req.AuthCode = auth_code

        self.reqid += 1
        self.api.ReqAuthenticate(req, self.reqid)

    def login(self) -> None:
        """用户登录"""
        if self.login_status:
            return

        req = CThostFtdcReqUserLoginField()
        req.BrokerID = self.broker
        req.UserID = self.user
        req.Password = self.pwd
        req.UserProductInfo = 'qts'
        ret = self.api.ReqUserLogin(req, self.next_reqid(), 0, None)
        if ret != 0:
            log.error(f"交易服务器登录失败，错误代码：{ret}")

    def send_order(self, req: OrderRequest) -> str:
        """委托下单"""
        if req.offset not in OFFSET_VT2CTP:
            log.error("请选择开平方向")
            return ""

        if req.type not in ORDERTYPE_VT2CTP:
            log.error(f"当前接口不支持该类型的委托{req.type.value}")
            return ""

        self.order_ref += 1

        tp: tuple = ORDERTYPE_VT2CTP[req.type]
        price_type, time_condition, volume_condition = tp

        ctp_req: dict = {
            "InstrumentID": req.symbol,
            "ExchangeID": req.exchange.value,
            "LimitPrice": req.price,
            "VolumeTotalOriginal": int(req.volume),
            "OrderPriceType": price_type,
            "Direction": DIRECTION_VT2CTP.get(req.direction, ""),
            "CombOffsetFlag": OFFSET_VT2CTP.get(req.offset, ""),
            "OrderRef": str(self.order_ref),
            "InvestorID": self.userid,
            "UserID": self.userid,
            "BrokerID": self.brokerid,
            "CombHedgeFlag": THOST_FTDC_HF_Speculation,
            "ContingentCondition": THOST_FTDC_CC_Immediately,
            "ForceCloseReason": THOST_FTDC_FCC_NotForceClose,
            "IsAutoSuspend": 0,
            "TimeCondition": time_condition,
            "VolumeCondition": volume_condition,
            "MinVolume": 1
        }

        self.reqid += 1
        n: int = self.reqOrderInsert(ctp_req, self.reqid)
        if n:
            self.gateway.write_log(f"委托请求发送失败，错误代码：{n}")
            return ""

        orderid: str = f"{self.frontid}_{self.sessionid}_{self.order_ref}"
        order: OrderData = req.create_order_data(orderid, self.gateway_name)
        self.gateway.on_order(order)

        return order.vt_orderid

    def cancel_order(self, req: CancelRequest) -> None:
        """委托撤单"""
        frontid, sessionid, order_ref = req.orderid.split("_")

        ctp_req: dict = {
            "InstrumentID": req.symbol,
            "ExchangeID": req.exchange.value,
            "OrderRef": order_ref,
            "FrontID": int(frontid),
            "SessionID": int(sessionid),
            "ActionFlag": THOST_FTDC_AF_Delete,
            "BrokerID": self.brokerid,
            "InvestorID": self.userid
        }

        self.reqid += 1
        self.reqOrderAction(ctp_req, self.reqid)

    def __run_query(self):
        log.info("start query...")
        self.query_settlement()
        sleep(1)
        self.query_account()
        sleep(1)
        self.query_contract()
        sleep(1)
        self.query_position()
        self.semaphore.acquire()
        log.info("finish query...")
        self.semaphore.release()


    def query_settlement(self):
        """查询结算单"""
        if not self.td_status:
            return
        self.semaphore.acquire()
        log.info("开始查询结算单...")
        req = CThostFtdcSettlementInfoConfirmField()
        req.BrokerID = self.broker
        req.InvestorID = self.user
        ret = self.api.ReqSettlementInfoConfirm(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询结算单失败，错误代码：{ret}")
            self.semaphore.release()

    def query_account(self) -> None:
        """查询资金"""
        if not self.td_status:
            return
        self.semaphore.acquire()
        log.info("开始查询资金...")
        req = CThostFtdcQryTradingAccountField()
        ret = self.api.ReqQryTradingAccount(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询资金失败，错误代码：{ret}")
            self.semaphore.release()

    def query_contract(self) -> None:
        if not self.td_status:
            return
    
        if len(self.gateway.contracts_map) > 0:
            return
        self.semaphore.acquire()
        log.info("开始查询合约...")
        req = CThostFtdcQryInstrumentField()
        ret = self.api.ReqQryInstrument(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询合约失败，错误代码：{ret}")
            self.semaphore.release()

    def query_position(self) -> None:
        """查询持仓"""
        if not self.td_status:
            return

        self.semaphore.acquire()
        log.warn("开始查询持仓...")

        req = CThostFtdcQryInvestorPositionField()
        req.BrokerID = self.broker
        req.InvestorID = self.user
        ret = self.api.ReqQryInvestorPosition(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询持仓失败，错误代码：{ret}")
            self.semaphore.release()

def adjust_price(price: float) -> float:
    """将异常的浮点数最大值（MAX_FLOAT）数据调整为0"""
    if price == MAX_FLOAT:
        price = 0
    return price


def get_data_path(folder_name: str):
    data_path = get_setting('data_path')
    folder_path = os.path.join(data_path, folder_name)
    if not os.path.exists(folder_path):
        os.makedirs(folder_path)
    return folder_path