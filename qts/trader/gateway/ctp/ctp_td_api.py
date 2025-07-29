import threading
import platform

from datetime import datetime
from time import sleep

from ..base_gateway import BaseGateway
from .base import *


class CtpTdApi(tdapi.CThostFtdcTraderSpi):
    """"""

    def __init__(self, gateway: BaseGateway) -> None:
        """构造函数"""
        super().__init__()

        self.gateway: BaseGateway = gateway
        self.acct_conf: AcctConf = gateway.acct_detail.acct_info.conf
        self.gateway_name: str = gateway.gateway_name
        __,self.address= self.acct_conf.td_addr.split('|')
        self.user = self.acct_conf.user
        self.pwd = self.acct_conf.pwd
        self.broker = self.acct_conf.broker
        self.auth = self.acct_conf.auth
        self.reqid: int = 0
        self.order_ref: int = 0
        self.connect_status: bool = False
        self.login_status: bool = False
        self.contract_inited: bool = False
        self.frontid: int = 0
        self.sessionid: int = 0
        self.trade_data: list[dict] = []
        self.position_map: dict[str, Position] = {}
        self.contract_map:dict[str,ContractData] = {}
        self.ordering_map: dict[str, OrderData] = {}
        self.api: tdapi.CThostFtdcTraderApi = None
        self.semaphore = threading.Semaphore(1)
        log.info(f"初始化交易接口,名称:{gateway.gateway_name}, 地址:{self.address}")

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

    def OnRspAuthenticate(self, pRspAuthenticateField: "CThostFtdcRspAuthenticateField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> None:
        """用户授权验证回报"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"交易服务器授权验证失败,{pRspInfo.ErrorMsg}")
            return

        log.info("交易服务器授权验证成功")
        self.login()

    def OnRspUserLogin(self, pRspUserLogin: "CThostFtdcRspUserLoginField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
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

    def OnRspOrderAction(self, pInputOrderAction: "CThostFtdcInputOrderActionField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """委托撤单失败回报"""
        log.error(f"交易撤单失败:{pRspInfo.ErrorMsg}")

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
                position: Position = self.position_map.get(key, None)
                if not position:
                    position = Position(
                        symbol=pInvestorPosition.InstrumentID,
                        exchange=contract.exchange,
                        direction=direction,
                    )
                    self.position_map[key] = position

                # 说明：
                # YdPosition是当前键值下的昨仓，是个静态值，不会更新。当前真实的昨仓值为Position-TodayPosition；
                #对于非上期/能源的交易所，合约的昨仓YdPosition和今仓TodayPosition在一条记录里面，而上期/能源是分成了两条记录。
                # 获取合约的乘数信息
                size: int = contract.multiple
                position.multiple = size

                volume: int = pInvestorPosition.Position
                td_volume: int = pInvestorPosition.TodayPosition if pInvestorPosition.TodayPosition else 0
                yd_volume: int = volume - td_volume
                
                #累计仓位
                position.volume += volume
                position.td_volume += td_volume
                position.yd_volume += yd_volume
                position.yestday_volume += pInvestorPosition.YdPosition

                # 计算持仓成本/均价
                position.hold_cost +=pInvestorPosition.PositionCost
                position.avg_price = round(position.hold_cost / (position.volume * size),2) if position.volume > 0 else 0
                position.hold_profit += round(pInvestorPosition.PositionProfit,2)
                position.close_profit += round(pInvestorPosition.CloseProfit,2)

                position.pre_price = pInvestorPosition.PreSettlementPrice
                position.margin += round(pInvestorPosition.UseMargin,2) + round(pInvestorPosition.FrozenMargin,2)
                position.commission += round(pInvestorPosition.Commission,2)
                # 更新仓位冻结数量
                if position.direction == PosDirection.LONG:
                    position.frozen += pInvestorPosition.ShortFrozen
                else:
                    position.frozen += pInvestorPosition.LongFrozen
                position.available = position.volume - position.frozen
                

        if bIsLast:
            self.gateway.on_positions(self.position_map)
            log.info(f"持仓信息查询成功,共{len(self.position_map)}条")
            self.position_map.clear()
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
        if pProduct.ProductClass in PRODUCT_CTP2VT:
            product: ProductData = ProductData(
                id = pProduct.ProductID,
                name = pProduct.ProductName,
                exchange = EXCHANGE_CTP2VT.get(pProduct.ExchangeID,None),
                type = PRODUCT_CTP2VT.get(pProduct.ProductClass, None),
                pricetick = pProduct.PriceTick,
                multiple = pProduct.VolumeMultiple
            )
        
            if product:
                self.gateway.on_product(product)
        if bIsLast:
            self.semaphore.release()


    def OnRspQryInstrument(self, pInstrument: "CThostFtdcInstrumentField", pRspInfo: "CThostFtdcRspInfoField",
                           nRequestID: "int", bIsLast: "bool") -> "void":
        """合约查询回报"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"查询合约失败,{pRspInfo.ErrorMsg}")
            self.semaphore.release()
            return

        product: ProductType = PRODUCT_CTP2VT.get(pInstrument.ProductClass, None)
        if product:
            contract: ContractData = ContractData(
                symbol=pInstrument.InstrumentID,
                exchange=EXCHANGE_CTP2VT[pInstrument.ExchangeID],
                name=pInstrument.InstrumentName,
                product=product,
                multiple=pInstrument.VolumeMultiple,
                pricetick=pInstrument.PriceTick,
                maxMarginSideAlgorithm=pInstrument.MaxMarginSideAlgorithm,
            )

            # 期权相关
            if contract.product == ProductType.OPTION:
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
                self.contract_map[contract.symbol] = contract

        if bIsLast:
            self.contract_inited = True
            log.info(f"合约信息查询成功,共{len(self.contract_map)}条")
            self.gateway.on_contracts(self.contract_map)
            self.contract_map.clear()
            self.semaphore.release()

    def OnRspQryInstrumentMarginRate(self, pInstrumentMarginRate: "CThostFtdcInstrumentMarginRateField", pRspInfo: "CThostFtdcRspInfoField", nRequestID: "int", bIsLast: "bool") -> "void":
        """请求查询合约保证金率响应"""
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            log.error(f"查询合约保证金率失败,{pRspInfo.ErrorMsg}")
            self.semaphore.release()
            return
        
        if pInstrumentMarginRate :
            contract: ContractData = self.gateway.get_contract(pInstrumentMarginRate.InstrumentID)
            if contract:
                contract.long_margin_by_volume = pInstrumentMarginRate.LongMarginRatioByVolume
                contract.long_margin_by_money = pInstrumentMarginRate.LongMarginRatioByMoney
                contract.short_margin_by_volume = pInstrumentMarginRate.ShortMarginRatioByVolume
                contract.short_margin_by_money = pInstrumentMarginRate.ShortMarginRatioByMoney
        if bIsLast:
            self.semaphore.release()
            log.info(f"合约保证金率查询成功")


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
            self.trade_data.append(trade)

        if bIsLast:
            log.info(f"成交信息查询成功")
            if len(self.trade_data) > 0:
                self.gateway.on_trades(self.trade_data)
                self.trade_data.clear()
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

        order: OrderData = self.ordering_map.get(order_ref,None)
        if not order:
            log.error(f"报单失败回报 {order_ref} 找不到委托请求")
            return
        order.status = Status.REJECTED
        order.status_msg = pRspInfo.ErrorMsg
        order.updatetimes = datetime.now()
        self.ordering_map.pop(order_ref,None)
        self.gateway.on_order(order)

    def OnRtnOrder(self, pOrder: tdapi.CThostFtdcOrderField) -> "void":
        """委托更新推送"""
        symbol: str = pOrder.InstrumentID;
        #contract: ContractData = self.gateway.get_contract(symbol)
        #OrderSubmitStatus为THOST_FTDC_OSS_InsertRejected（”报单已经被拒绝“）即说明该笔单子被交易所拒单（处于被动撤单状态）。

        status: Status = STATUS_CTP2VT.get(pOrder.OrderStatus, None)
        if not status:
            log.error(f"收到不支持的委托状态，委托号：{order_ref}")
            return
        order_ref: str = pOrder.OrderRef
        order: OrderData = self.ordering_map.get(order_ref,None)
        if not order:
            log.error(f"报单回报 {order_ref} 找不到委托请求")
            return
        order.order_sys_id = pOrder.OrderSysID
        order.status = status
        order.status_msg = pOrder.StatusMsg
        order.traded = pOrder.VolumeTraded
        order.updatetimes = datetime.now()
        if not order.is_active():           
            self.ordering_map.pop(order_ref,None)
        self.gateway.on_order(order)

    def OnRtnTrade(self, pTrade: tdapi.CThostFtdcTradeField) -> None:
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
            flow_path = (str(path) + "/td")
            self.api = tdapi.CThostFtdcTraderApi.CreateFtdcTraderApi(flow_path)
            self.api.RegisterSpi(self)
            self.api.RegisterFront(self.address)
            self.api.SubscribePrivateTopic(tdapi.THOST_TERT_QUICK)
            self.api.SubscribePublicTopic(tdapi.THOST_TERT_QUICK)
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
        req = tdapi.CThostFtdcReqAuthenticateField()
        req.BrokerID = self.broker
        req.UserID = self.user
        req.AppID = appid
        req.AuthCode = auth_code

        ret = self.api.ReqAuthenticate(req, 0)
        log.debug(f"ReqAuthenticate ret:{ret}")

    def login(self) -> None:
        """用户登录"""
        if self.login_status:
            return

        req = tdapi.CThostFtdcReqUserLoginField()
        req.BrokerID = self.broker
        req.UserID = self.user
        req.Password = self.pwd
        req.UserProductInfo = 'qts'
        
        if platform.system() == 'Darwin':
            ret = self.api.ReqUserLogin(req, self.next_reqid(), 0, None)
        else:
            ret = self.api.ReqUserLogin(req, self.next_reqid())
        
        if ret != 0:
            log.error(f"交易服务器登录失败，错误代码：{ret}")

    def insert_order(self, req: OrderData) -> bool:
        """委托下单"""
        if req.offset not in OFFSET_VT2CTP:
            log.error(f"开平方向无效{req.offset.value}")
            return ""

        ctp_req: tdapi.CThostFtdcInputOrderField = tdapi.CThostFtdcInputOrderField()
        ctp_req.RequestID = self.next_reqid()
        ctp_req.OrderRef = req.order_ref
        ctp_req.InvestorID = self.user
        ctp_req.BrokerID = self.broker
        ctp_req.InstrumentID = req.symbol
        ctp_req.ExchangeID = req.exchange.value
        ctp_req.Direction = DIRECTION_VT2CTP.get(req.direction, "")
        ctp_req.CombOffsetFlag = OFFSET_VT2CTP.get(req.offset, "")
        ctp_req.CombHedgeFlag = tdapi.THOST_FTDC_HF_Speculation #组合投机套保标志
        ctp_req.ContingentCondition = tdapi.THOST_FTDC_CC_Immediately
        ctp_req.ForceCloseReason = tdapi.THOST_FTDC_FCC_NotForceClose #强平原因: 非强平
        ctp_req.IsAutoSuspend = 0 #自动挂起标志 0:no 1:yes

        ctp_req.OrderPriceType = tdapi.THOST_FTDC_OPT_LimitPrice #委托价格类型: 限价
        ctp_req.LimitPrice = req.price
        ctp_req.VolumeTotalOriginal = int(req.volume)
        ctp_req.MinVolume = 1
    

        if req.type == OrderType.FAK:
            ctp_req.TimeCondition = tdapi.THOST_FTDC_TC_IOC # 委托有效期: 立即成交剩余转限价
            ctp_req.VolumeCondition = tdapi.THOST_FTDC_VC_AV 
        elif req.type == OrderType.FOK:
            ctp_req.TimeCondition = tdapi.THOST_FTDC_TC_IOC
            ctp_req.VolumeCondition = tdapi.THOST_FTDC_VC_CV 
        else:
            ctp_req.TimeCondition = tdapi.THOST_FTDC_TC_GFD  # 委托有效期: 当日有效
            ctp_req.VolumeCondition = tdapi.THOST_FTDC_VC_AV 

        
        n: int = self.api.ReqOrderInsert(ctp_req, self.reqid)
        if n:
            log.error(f"OrderRef:{req.order_ref} 委托请求发送失败，错误代码：{n}")
            return False
        self.ordering_map[req.order_ref] = req
        return True

    def cancel_order(self, req: OrderCancel) -> None:
        """委托撤单"""
        
        ctp_req:tdapi.CThostFtdcInputOrderActionField = tdapi.CThostFtdcInputOrderActionField()
        ctp_req.InvestorID=self.user
        ctp_req.BrokerID=self.broker
        ctp_req.ActionFlag = tdapi.THOST_FTDC_AF_Delete
        if not req.order_sys_id:
            ctp_req.OrderRef = req.order_ref
            ctp_req.FrontID = int(self.frontid)
            ctp_req.SessionID = int(self.sessionid)
        else:
            ctp_req.OrderSysID = req.order_sys_id
            ctp_req.ExchangeID = req.exchange.value

        self.api.ReqOrderAction(ctp_req, self.next_reqid())

    def __run_query(self):
        # log.info(f"[1/6] 查询结算信息")
        # self.query_settlement()
        # sleep(1)
        # log.info(f"[2/6] 查询账户信息")
        # self.query_account()
        # sleep(1)
        # log.info(f"[3/6] 查询产品信息")
        # self.query_product()
        # sleep(1)
        # log.info(f"[4/6] 查询合约信息")
        # self.query_contract()
        # sleep(1)
        # log.info(f"[5/6] 查询持仓信息")
        # self.query_position()
        # sleep(1)
        # log.info(f"[6/6] 查询成交信息")
        # self.query_trades()
        # sleep(1)
        # self.semaphore.acquire()
        # log.info("查询完成！")
        # self.semaphore.release()
        # self.gateway.on_ready()

        self.query_functions: list = [
            self.query_settlement, 
            self.query_account, 
            self.query_product, 
            self.query_contract, 
            self.query_margin,
            self.query_position,
            self.query_trades]

        process: int = 0
        for query_func in self.query_functions:
            process +=1
            #self.semaphore.acquire()
            sleep(1)
            log.info(f"查询进度:[{process}/{len(self.query_functions)}]")
            query_func()
        self.semaphore.acquire()
        log.info("查询完成！")
        self.semaphore.release()
        self.gateway.on_ready()



    def query_settlement(self):
        """查询结算单"""
        if not self.td_status:
            return
        self.semaphore.acquire()
        log.info("开始查询结算单...")
        req = tdapi.CThostFtdcSettlementInfoConfirmField()
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
        req = tdapi.CThostFtdcQryTradingAccountField()
        ret = self.api.ReqQryTradingAccount(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询资金失败，错误代码：{ret}")
            self.semaphore.release()

    def query_product(self) ->None:
        if not self.td_status:
            return
        self.semaphore.acquire()
        log.info("开始查询产品...")
        req = tdapi.CThostFtdcQryProductField()
        ret = self.api.ReqQryProduct(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询产品失败，错误代码：{ret}")
            self.semaphore.release()
    
    def query_contract(self) -> None:
        if not self.td_status:
            return
        log.info("开始查询合约...")
        if len(self.gateway.get_contracts_map()) > 0:
            return
        self.semaphore.acquire()
        req = tdapi.CThostFtdcQryInstrumentField()
        ret = self.api.ReqQryInstrument(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询合约失败，错误代码：{ret}")
            self.semaphore.release()

    def query_position(self) -> None:
        """查询持仓"""
        if not self.td_status:
            return

        self.semaphore.acquire()
        log.info("开始查询持仓...")

        req = tdapi.CThostFtdcQryInvestorPositionField()
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
        req = tdapi.CThostFtdcQryTradeField()
        req.BrokerID = self.broker
        req.InvestorID = self.user
        ret = self.api.ReqQryTrade(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询成交失败，错误代码：{ret}")
            self.semaphore.release()
    
    def query_margin(self):
        """查询保证金"""
        if not self.td_status:
            return
        self.semaphore.acquire()
        log.info("开始查询保证金...")
        req:tdapi.CThostFtdcQryInstrumentMarginRateField = tdapi.CThostFtdcQryInstrumentMarginRateField()
        req.BrokerID = self.broker
        req.InvestorID = self.user
        req.HedgeFlag = tdapi.THOST_FTDC_HF_Speculation
        ret = self.api.ReqQryInstrumentMarginRate(req, self.next_reqid())
        if ret != 0:
            log.error(f"查询保证金失败，错误代码：{ret}")
            self.semaphore.release()

    def next_reqid(self)->int:
        self.reqid +=1
        return self.reqid

