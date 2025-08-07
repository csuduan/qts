import os
import pickle
import datetime
import threading
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional, Callable
from copy import copy

from qts.common.event import event_engine,Event
from qts.common.constant import *
from qts.common.object import *
from qts.common.message import MsgType
from qts.common import get_logger,get_config

from .utils import get_pos_direction

log = get_logger(__name__)


class BaseGateway(ABC):   
    # Default name for the gateway.
    default_name: str = ""
    # Exchanges supported in the gateway.
    exchanges: List[Exchange] = []

    def __init__(self, acct_detail: AcctDetail) -> None:
        self.acct_detail: AcctDetail = acct_detail
        self.gateway_name: str = self.acct_detail.acct_info.id
        self.order_ref:int = 1
        self.__load_cache()


    def trig_event(self, type: MsgType, data: Any = None) -> None:
        event_engine.put(type,data)

    def on_ready(self) -> None:
        self.trig_event(MsgType.ON_READY,self.acct_detail)
    def on_status(self,status:StatusData) -> None:
        if status.type == "td":
            self.acct_detail.acct_info.td_status = status.status
        elif status.type == "md":
            self.acct_detail.acct_info.md_status = status.status
        if status.trading_day is not None:
            self.acct_detail.acct_info.trading_day = status.trading_day
        if status.order_ref > 0  :
            self.order_ref = status.order_ref
        self.trig_event(MsgType.ON_STATUS, {})

    def on_tick(self, tick: TickData) -> None:
        self.acct_detail.tick_map[tick.symbol] = tick
        #不动态更新持仓盈亏
        self.trig_event(MsgType.ON_TICK, tick)

    def on_trade(self, trade: TradeData) -> None:
        #需要根据trade.symbol 找到对应的position，然后更新position的持仓
        self.acct_detail.trade_map[trade.id] = trade
        pos :Position = self.get_position(trade.symbol,trade.offset,trade.direction,trade.exchange)
        #更新持仓
        if pos.frozen !=0:
            #解除冻结
            pos.frozen -= trade.volume if trade.offset == Offset.OPEN else -trade.volume
        #更新持仓
        if trade.offset == Offset.OPEN:
            #开仓
            pos.td_volume += trade.volume
            pos.volume += trade.volume
            #重新计算成本
            pos.hold_cost += round(trade.volume*trade.price*pos.multiple,2)
            pos.avg_price = round(pos.hold_cost/pos.volume/pos.multiple,2)
        else:
            #平仓
            if trade.offset == Offset.CLOSETODAY:
                pos.td_volume -= trade.volume
                pos.volume -= trade.volume
            else:
                #优先平昨
                yd = min(trade.volume,pos.yd_volume)
                td = trade.volume - yd
                pos.yd_volume -= yd
                pos.td_volume -= td
                pos.volume -= trade.volume
            pos.close_profit += round((trade.price-pos.avg_price)*trade.volume*pos.multiple*(-1 if trade.direction == Direction.SELL else 1),2)
            #重新计算成本
            pos.hold_cost -= round(trade.volume*pos.multiple*pos.avg_price,2)  

        log.info(f"成交回报: {trade.order_ref} {trade.symbol} {trade.offset.value}  {trade.direction.value} {trade.volume}  trade_id:{trade.id}")           
        self.trig_event(MsgType.ON_TRADE, trade)
        self.trig_event(MsgType.ON_POSITION,pos)


    def on_order(self, order: OrderData) -> None:
        #暂不处理非本应用发起的报单回报
        #保证金、手续费、盈亏由管理平台统一计算
        if order.order_ref not in self.acct_detail.order_map:
            return

        pos = self.get_position(order.symbol,order.offset,order.direction,order.exchange)
        if order.status == Status.CANCELLED:
            #解除冻结
            untraded = order.volume-order.traded
            pos.frozen -= untraded if order.offset == Offset.OPEN else -untraded
        if not order.is_active():
            order.deleted = True
            self.acct_detail.order_map.pop(order.order_ref,None)
        msg = f"报单回报: {order.order_ref} {order.symbol} {order.offset.value}  {order.direction.value} {order.traded}/{order.volume}  {order.status_msg} sys_id:{order.order_sys_id}"
        if "拒绝" in order.status_msg:
            log.error(msg)
        else:
            log.info(msg)           
        self.trig_event(MsgType.ON_ORDER, order)
        self.trig_event(MsgType.ON_POSITION, pos)

    def on_positions(self, positions:dict[str,Position]) -> None:
        self.acct_detail.position_map.clear()
        self.acct_detail.position_map.update(positions)
        self.trig_event(MsgType.ON_POSITIONS, positions)

    def on_trades(self, trades: List[TradeData]) -> None:
        self.acct_detail.trade_map.clear()
        for trade in trades:
            self.acct_detail.trade_map[trade.id] = trade
        self.trig_event(MsgType.ON_TRADES, trades)
    

    def on_account(self, account: AccountData) -> None:
        self.acct_detail.acct_info.balance = account.balance
        self.acct_detail.acct_info.frozen = account.frozen
        self.trig_event(MsgType.ON_ACCT_INFO, account)

    def on_contracts(self, contracts: dict[str,ContractData]) -> None:
        self.acct_detail.contracts_map.update(contracts)
        self.trig_event(MsgType.ON_CONTRACTS, contracts)
        self.__write_cache()
    
    def on_product(self,product:ProductData)->None:
        self.acct_detail.product_map[product.id] = product
        pass


    @abstractmethod
    def connect(self) -> None:
        pass

    @abstractmethod
    def disconnect(self) -> None:
        pass

    @abstractmethod
    def subscribe(self, req: SubscribeRequest) -> None:
        pass

    @abstractmethod
    def create_order(self,req: OrderRequest) -> OrderData:
        pass

    @abstractmethod
    def send_order(self, req: OrderData) -> str:
        pass

    @abstractmethod
    def cancel_order(self, req: OrderCancel) -> None:
        pass


    def get_default_setting(self) -> Dict[str, Any]:
        return self.default_setting
    
    def get_contract(self, symbol: str) -> ContractData:
        return self.acct_detail.contracts_map.get(symbol, None)
    
    def get_contracts_map(self) -> Dict[str, ContractData]:
        return self.acct_detail.contracts_map

    def __load_cache(self):
        #加载合约缓存
        data_path = get_config('data_path')
        contracts_path = os.path.join(data_path, 'contracts.pkl')
        if os.path.exists(contracts_path):
            with open(contracts_path, 'rb') as f:
                data = pickle.load(f)
                contracts_map: dict[str, ContractData] = data['contracts']
                contracts_date: str = data['date']
                if contracts_date == datetime.today().date() and contracts_map is not None:
                    # 当日缓存过合约不信，则直接加载
                    self.acct_detail.contracts_map.clear()
                    self.acct_detail.contracts_map.update(contracts_map)
    
    def __write_cache(self):
        data_path = get_config('data_path')
        contracts_path = os.path.join(data_path, 'contracts.pkl')
        with open(contracts_path, 'wb') as f:
            pickle.dump({'contracts': self.acct_detail.contracts_map, 'date': datetime.today().date()}, f)

    def get_position(self,symbol:str,offset:Offset,direction:Direction,exchange:Exchange) -> Position:
        """获取持仓"""
        pos_direction = get_pos_direction(offset,direction)
        pos_id = f"{symbol}-{pos_direction.value}"
        if pos_id not in self.acct_detail.position_map:
            pos= Position(id=pos_id,symbol=symbol,direction=pos_direction,exchange=exchange)
            contract = self.get_contract(symbol)
            pos.multiple = contract.multiple
            self.acct_detail.position_map[pos_id] = pos
        return self.acct_detail.position_map[pos_id]
    
    def next_order_ref(self) -> int:
        """
        线程安全地获取下一个order_ref
        """
        with threading.Lock():
            self.order_ref += 1
            return self.order_ref
    




    

