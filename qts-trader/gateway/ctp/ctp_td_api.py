import os
import queue
import threading
from datetime import datetime
from pathlib import Path
from time import sleep
from typing import Dict

from config import get_setting
from qts.model.object import *
from qts.model.constant import *
from qts.log import get_logger

from ..base_gateway import BaseGateway
from .lib import *

log = get_logger(__name__)


class CtpTdApi(CThostFtdcTraderSpi):
    """"""

    def __init__(self, gateway: BaseGateway) -> None:
        """构造函数"""
        super().__init__()

        self.gateway: BaseGateway = gateway
        self.acct_conf: AcctConf = gateway.acct_detail.acct_info.conf
        self.gateway_name: str = gateway.gateway_name

        __,self.address= self.acct_conf.td_addr.split('|')
        self.user,self.pwd = self.acct_conf.user.split('|')
        self.broker = self.acct_conf.broker
        self.auth = self.acct_conf.auth

        self.reqid: int = 0
        self.order_ref: int = 0
        self.connect_status: bool = False
        self.login_status: bool = False
        self.contract_inited: bool = False
        self.frontid: int = 0
        self.sessionid: int = 0
        self._trade_data: list[dict] = []
        self._positions: dict[str, Position] = {}
        self._contract_map:dict[str,ContractData] = {}
        self._ordering_map: dict[str, OrderData] = {}

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
        # 需要根据sessionid，MaxOrderRef 生成order_ref   
        #self.order_ref = int(pRspUserLogin.MaxOrderRef)+1
        unsigned_sessionid = self.sessionid & 0xFFFF  # Keep only lower 16 bits
        self.order_ref = (unsigned_sessionid << 16) + int(pRspUserLogin.MaxOrderRef) + 1
        self.gateway.on_status(StatusData(type="td",status=True,trading_day=self.trading_day,order_ref=self.order_ref))
        log.info(f"交易服务器登录成功！ {self.trading_day} frontId:{self.frontid} sessionId:{self.sessionid} orderRef:{self.order_ref}")

        # 异步发起查询
        threading.Thread(target=self.__run_query).start()

    

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
            contract: ContractData = self.gateway.get_contract(symbol)
            direction: PosDirection = POS_DIRECTION_CTP2VT[pInvestorPosition.PosiDirection]

            if contract:
                # 获取之前缓存的持仓数据缓存
                key: str = f"{pInvestorPosition.InstrumentID}-{direction.value}"
                position: Position = self._positions.get(key, None)
                if not position:
                    position = Position(
                        id = key,
                        symbol=pInvestorPosition.InstrumentID,
                        exchange=contract.exchange,
                        direction=direction,
                    )
                    self._positions[key] = position

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
                position.price = round(position.hold_cost / (position.volume * size),2) if position.volume > 0 else 0
                position.hold_profit += round(pInvestorPosition.PositionProfit,2)
                position.close_profit += round(pInvestorPosition.CloseProfit,2)

                position.pre_price = pInvestorPosition.PreSettlementPrice
                position.margin += round(pInvestorPosition.UseMargin,2)
                position.commission += round(pInvestorPosition.Commission,2)
                # 更新仓位冻结数量
                if position.direction == PosDirection.LONG:
                    position.frozen += pInvestorPosition.ShortFrozen
                else:
                    position.frozen += pInvestorPosition.LongFrozen
                position.available = position.volume - position.frozen
                

        if bIsLast:
            self.gateway.on_positions(self._positions)
            log.info(f"持仓信息查询成功,共{len(self._positions)}条")
            self._positions.clear()
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
                self._contract_map[contract.symbol] = contract

        if bIsLast:
            self.contract_inited = True
            log.info(f"合约信息查询成功,共{len(self._contract_map)}条")
            self.gateway.on_contracts(self._contract_map)
            self._contract_map.clear()
            self.semaphore.release()

    def OnRspQryTrade(self, pTrade: "CThostFtdcTradeField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """请求查询成交响应"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"查询成交失败,{pRspInfo.ErrorMsg}")
            self.semaphore.release()
            return

        if pTrade:
            symbol: str = pTrade.InstrumentID
            order_ref: str = pTrade.OrderRef

            trade: TradeData = TradeData(
                id=pTrade.TradeID,
                trading_day=pTrade.TradeDate,
                symbol=symbol,
                exchange=EXCHANGE_CTP2VT[pTrade.ExchangeID],
                order_ref=order_ref,
                direction=DIRECTION_CTP2VT[pTrade.Direction],
                offset=OFFSET_CTP2VT[pTrade.OffsetFlag],
                price=pTrade.Price,
                volume=pTrade.Volume,
                time=pTrade.TradeTime          
                )
            self._trade_data.append(trade)

        if bIsLast:
            log.info(f"成交信息查询成功")
            if len(self._trade_data) > 0:
                self.gateway.on_trades(self._trade_data)
                self._trade_data.clear()
            self.semaphore.release()
        

    def OnRspQryOrder(self, pOrder: "CThostFtdcOrderField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """请求查询报单响应"""
        pass

    def OnRspOrderInsert(self, pInputOrder: "CThostFtdcInputOrderField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """委托下单失败回报"""
        if not pRspInfo :
            return
        order_ref: str = pInputOrder.OrderRef
        log.error(f"报单失败回报 {order_ref} {pRspInfo.ErrorID} {pRspInfo.ErrorMsg}")

        order: OrderData = self._ordering_map.get(order_ref,None)
        if not order:
            log.error(f"报单失败回报 {order_ref} 找不到委托请求")
            return
        order.status = Status.REJECTED
        order.status_msg = pRspInfo.ErrorMsg
        order.updatetimes = datetime.now()
        self._ordering_map.pop(order_ref,None)
        self.gateway.on_order(order)

    def OnRtnOrder(self, pOrder: "CThostFtdcOrderField") -> "void":
        """委托更新推送"""
        symbol: str = pOrder.InstrumentID;
        #contract: ContractData = self.gateway.get_contract(symbol)

        status: Status = STATUS_CTP2VT.get(pOrder.OrderStatus, None)
        if not status:
            log.error(f"收到不支持的委托状态，委托号：{order_ref}")
            return
        order_ref: str = pOrder.OrderRef
        order: OrderData = self._ordering_map.get(order_ref,None)
        if not order:
            log.error(f"报单回报 {order_ref} 找不到委托请求")
            return
        order.order_sys_id = pOrder.OrderSysID
        order.status = status
        order.status_msg = pOrder.StatusMsg
        order.traded = pOrder.VolumeTraded
        order.updatetimes = datetime.now()
        if not order.is_active():           
            self._ordering_map.pop(order_ref,None)
        self.gateway.on_order(order)

    def OnRtnTrade(self, pTrade: "CThostFtdcTradeField") -> None:
        """成交数据推送"""

        trade: TradeData = TradeData(
            id=pTrade.TradeID,
            trading_day=pTrade.TradeDate,
            symbol=pTrade.InstrumentID,
            exchange=EXCHANGE_CTP2VT[pTrade.ExchangeID],
            order_ref=pTrade.OrderRef,
            direction=DIRECTION_CTP2VT[pTrade.Direction],
            offset=OFFSET_CTP2VT[pTrade.OffsetFlag],
            price=pTrade.Price,
            volume=pTrade.Volume,
            time=pTrade.TradeTime
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

    def insert_order(self, req: OrderData) -> bool:
        """委托下单"""
        if req.offset not in OFFSET_VT2CTP:
            log.error(f"开平方向无效{req.offset.value}")
            return ""

        self.reqid += 1
        ctp_req: CThostFtdcInputOrderField = CThostFtdcInputOrderField()
        ctp_req.RequestID = self.reqid
        ctp_req.OrderRef = req.order_ref
        ctp_req.InvestorID = self.user
        ctp_req.BrokerID = self.broker
        ctp_req.InstrumentID = req.symbol
        ctp_req.ExchangeID = req.exchange.value
        ctp_req.Direction = DIRECTION_VT2CTP.get(req.direction, "")
        ctp_req.CombOffsetFlag = OFFSET_VT2CTP.get(req.offset, "")
        ctp_req.CombHedgeFlag = THOST_FTDC_HF_Speculation #组合投机套保标志
        ctp_req.ContingentCondition = THOST_FTDC_CC_Immediately
        ctp_req.ForceCloseReason = THOST_FTDC_FCC_NotForceClose #强平原因: 非强平
        ctp_req.IsAutoSuspend = 0 #自动挂起标志 0:no 1:yes

        ctp_req.OrderPriceType = THOST_FTDC_OPT_LimitPrice #委托价格类型: 限价
        ctp_req.LimitPrice = req.price
        ctp_req.VolumeTotalOriginal = int(req.volume)
        ctp_req.MinVolume = 1
    

        if req.type == OrderType.FAK:
            ctp_req.TimeCondition = THOST_FTDC_TC_IOC # 委托有效期: 立即成交剩余转限价
            ctp_req.VolumeCondition = THOST_FTDC_VC_AV 
        elif req.type == OrderType.FOK:
            ctp_req.TimeCondition = THOST_FTDC_TC_IOC
            ctp_req.VolumeCondition = THOST_FTDC_VC_CV 
        else:
            ctp_req.TimeCondition = THOST_FTDC_TC_GFD  # 委托有效期: 当日有效
            ctp_req.VolumeCondition = THOST_FTDC_VC_AV 

        
        n: int = self.api.ReqOrderInsert(ctp_req, self.reqid)
        if n:
            log.error(f"OrderRef:{req.order_ref} 委托请求发送失败，错误代码：{n}")
            return False
        self._ordering_map[req.order_ref] = req
        return True

    def cancel_order(self, req: OrderCancel) -> None:
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
        sleep(1)
        self.query_trades()
        sleep(1)
        self.semaphore.acquire()
        log.info("finish query...")
        self.semaphore.release()
        self.gateway.on_ready()


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
    
        if len(self.gateway.get_contracts_map()) > 0:
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
    
    def query_trades(self):
        """查询成交"""
        if not self.td_status:
            return
        self.semaphore.acquire()
        log.info("开始查询成交...")
        req = CThostFtdcQryTradeField()
        req.BrokerID = self.broker
        req.InvestorID = self.user
        ret = self.api.ReqQryTrade(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询成交失败，错误代码：{ret}")
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