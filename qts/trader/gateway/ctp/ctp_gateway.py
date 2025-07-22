from datetime import datetime

from qts.common.message import MsgType
from qts.common import get_logger

from ..base_gateway import BaseGateway
from .ctp_td_api import CtpTdApi
from .ctp_md_api import CtpMdApi
from .base import *


log = get_logger(__name__)

class CtpGateway(BaseGateway):
    """
    CTP交易网关。
    """

    default_name: str = "CTP"
    exchanges: list[str] = list(EXCHANGE_CTP2VT.values())

    def __init__(self, acct_detail:AcctDetail) -> None:
        """构造函数"""
        super().__init__(acct_detail)

        self.td_api: CtpTdApi = CtpTdApi(self)
        self.md_api: CtpMdApi = CtpMdApi(self)

         
    def connect(self) -> None:
        '''连接接口'''
        self.td_api.connect()
        self.md_api.connect()

    def disconnect(self) -> None:
        """断开接口"""
        self.td_api.close()
        self.md_api.close()

    def subscribe(self, req: SubscribeRequest) -> None:
        """订阅行情"""
        self.md_api.subscribe(req)

    def create_order(self,req: OrderRequest) -> OrderData:
        """创建订单"""
        order_ref = self.next_order_ref()
        if not req.exchange or req.exchange == Exchange.NONE:
            contract:ContractData = self.acct_detail.contracts_map[req.symbol]
            req.exchange = contract.exchange
        trading_day = self.acct_detail.acct_info.trading_day
        order:OrderData = OrderData(
            order_ref=str(order_ref),
            symbol=req.symbol,
            exchange=req.exchange,
            order_sys_id="",
            type=req.type,
            direction=req.direction,
            offset=req.offset,
            price=req.price,
            volume=req.volume,
            traded=0,
            status=Status.SUBMITTING,
            trading_day=trading_day,
            time=datetime.now().strftime("%H:%M:%S"),
            updatetimes=datetime.now()
        )
        if req.price == 0 and req.symbol in self.acct_detail.tick_map:
            tick =self.acct_detail.tick_map[req.symbol]
            order.price = tick.ask_price_1 if order.direction == Direction.BUY else tick.bid_price_1
        return order

    def send_order(self, req: OrderData) -> bool:
        """委托下单"""

        if not req.order_ref:
            req.order_ref = self.next_order_ref() 
        #冻结仓位检查
        pos : Position = self.get_position(req.symbol,req.offset,req.direction,req.exchange)
        if pos.frozen<0 and req.offset == Offset.OPEN or pos.frozen>0 and req.offset == Offset.CLOSE:
            log.error(f"委托失败，{req.symbol} {req.direction.value} 有反向冻结。。。")
            return False
        
        #自成交检查
        if  self.__autotrade_check(req):
            log.error(f"委托失败，{req.symbol} {req.direction.value} 有自成交风险")
            return False
        # 非上期所调整类型（除了上期所/能源中心外，不区分平今平昨，平仓统一使用THOST_FTDC_OF_Close。）
        if req.exchange not in [Exchange.SHFE,Exchange.INE] and req.offset != Offset.OPEN:
            req.offset = Offset.CLOSE
        
        ret = self.td_api.insert_order(req)
        if ret:
            self.acct_detail.order_map[req.order_ref] = req
            #冻结仓位          
            pos.frozen += req.volume if req.offset == Offset.OPEN else -req.volume
            log.info(f"委托成功，{req.order_ref} {req.symbol} {req.offset.value}  {req.direction.value} {req.volume} {req.price}")
            self.trig_event(MsgType.ON_ORDER, req)
            self.trig_event(MsgType.ON_POSITION, pos)
        return ret

    def cancel_order(self, req: OrderCancel) -> None:
        """委托撤单"""
        self.td_api.cancel_order(req)

    def query_account(self) -> None:
        """查询资金"""
        self.td_api.query_account()

    def query_position(self) -> None:
        """查询持仓"""
        self.td_api.query_position()
    
    def __autotrade_check(self,req:OrderData) -> bool:
        """自成交检查"""
        #高买低卖有自成交风险
        if req.direction == Direction.BUY:
            # Get minimum sell price from all SELL orders
            min_sell_price = float('inf')
            for order in self.acct_detail.order_map.values():
                if order.direction == Direction.SELL and order.status in ACTIVE_STATUSES:
                    min_sell_price = min(min_sell_price, order.price)
            return req.price >= min_sell_price
        else:
            # Get maximum buy price from all BUY orders
            max_buy_price = float('-inf')
            for order in self.acct_detail.order_map.values():
                if order.direction == Direction.BUY and order.status in ACTIVE_STATUSES:
                    max_buy_price = max(max_buy_price, order.price)
            return req.price <= max_buy_price


    
