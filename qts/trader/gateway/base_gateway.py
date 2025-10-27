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
    """
    交易网关基类
    
    所有具体交易网关（如CTP、易盛等）都需要继承此类，并实现抽象方法
    负责处理交易连接、行情订阅、订单管理等核心功能
    """
    #default_name: str = ""
    #exchanges: List[Exchange] = []

    def __init__(self, acct_detail: AcctDetail) -> None:
        """
        初始化网关
        
        Args:
            acct_detail: 账户详细信息，包含账户配置、持仓、合约等数据
        """
        self.acct_detail: AcctDetail = acct_detail
        self.gateway_name: str = self.acct_detail.acct_info.id
        self.order_ref:int = 1
        self.__load_cache()


    def trig_event(self, type: MsgType, data: Any = None) -> None:
        """
        触发事件
        
        Args:
            type: 消息类型
            data: 事件数据
        """
        event_engine.put(type,data)

    def on_ready(self) -> None:
        """网关就绪回调，通知系统网关已准备就绪"""
        self.trig_event(MsgType.ON_READY,self.acct_detail)

    def on_status(self,status:StatusData) -> None:
        """
        状态更新回调
        
        Args:
            status: 状态数据，包含交易状态、行情状态、交易日等信息
        """
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
        """
        行情数据回调
        
        Args:
            tick: 行情数据，包含最新价、成交量、持仓量等信息
        """
        self.acct_detail.tick_map[tick.symbol] = tick
        #不动态更新持仓盈亏
        self.trig_event(MsgType.ON_TICK, tick)

    def on_trade(self, trade: TradeData) -> None:
        """
        成交回报回调
        
        Args:
            trade: 成交数据，包含成交价格、成交量、方向等信息
        """
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
        """
        订单状态回调
        
        Args:
            order: 订单数据，包含订单状态、成交数量、状态信息等
        """
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
        """
        持仓查询回调
        
        Args:
            positions: 持仓数据字典，key为持仓ID，value为持仓数据
        """
        self.acct_detail.position_map.clear()
        self.acct_detail.position_map.update(positions)
        self.trig_event(MsgType.ON_POSITIONS, positions)

    def on_trades(self, trades: List[TradeData]) -> None:
        """
        成交查询回调
        
        Args:
            trades: 成交数据列表
        """
        self.acct_detail.trade_map.clear()
        for trade in trades:
            self.acct_detail.trade_map[trade.id] = trade
        self.trig_event(MsgType.ON_TRADES, trades)
    

    def on_account(self, account: AccountData) -> None:
        """
        账户查询回调
        
        Args:
            account: 账户数据，包含余额、冻结资金等信息
        """
        self.acct_detail.acct_info.balance = account.balance
        self.acct_detail.acct_info.frozen = account.frozen
        self.acct_detail.acct_info.available = account.available
        self.acct_detail.acct_info.hold_profit = account.hold_profit
        self.acct_detail.acct_info.close_profit = account.close_profit
        self.acct_detail.acct_info.pre_balance = account.pre_balance
        self.trig_event(MsgType.ON_ACCT_INFO, account)

    def on_contracts(self, contracts: dict[str,ContractData]) -> None:
        """
        合约查询回调
        
        Args:
            contracts: 合约数据字典，key为合约代码，value为合约数据
        """
        self.acct_detail.contracts_map.update(contracts)
        self.trig_event(MsgType.ON_CONTRACTS, contracts)
        self.__write_cache()
    
    def on_product(self,product:ProductData)->None:
        """
        产品查询回调
        
        Args:
            product: 产品数据
        """
        self.acct_detail.product_map[product.id] = product
        pass


    @abstractmethod
    def connect(self) -> None:
        """连接交易接口，子类必须实现此方法"""
        pass

    @abstractmethod
    def disconnect(self) -> None:
        """断开交易接口连接，子类必须实现此方法"""
        pass

    @abstractmethod
    def subscribe(self, req: SubscribeRequest) -> None:
        """
        订阅行情
        
        Args:
            req: 订阅请求，包含要订阅的合约代码等信息
        """
        pass

    @abstractmethod
    def create_order(self,req: OrderRequest) -> OrderData:
        """
        创建订单
        
        Args:
            req: 订单请求，包含合约、方向、价格、数量等信息
            
        Returns:
            OrderData: 创建的订单数据
        """
        pass

    @abstractmethod
    def send_order(self, req: OrderData) -> str:
        """
        发送订单
        
        Args:
            req: 订单数据
            
        Returns:
            str: 订单系统ID
        """
        pass

    @abstractmethod
    def cancel_order(self, req: OrderCancel) -> None:
        """
        撤销订单
        
        Args:
            req: 撤单请求，包含要撤销的订单信息
        """
        pass


    def get_contract(self, symbol: str) -> ContractData:
        """
        获取合约信息
        
        Args:
            symbol: 合约代码
            
        Returns:
            ContractData: 合约数据，如果不存在则返回None
        """
        return self.acct_detail.contracts_map.get(symbol, None)
    
    def get_contracts_map(self) -> Dict[str, ContractData]:
        """
        获取所有合约信息
        
        Returns:
            Dict[str, ContractData]: 合约数据字典
        """
        return self.acct_detail.contracts_map

    

    def get_position(self,symbol:str,offset:Offset,direction:Direction,exchange:Exchange) -> Position:
        """
        获取持仓信息，如果不存在则创建新的持仓
        
        Args:
            symbol: 合约代码
            offset: 开平方向
            direction: 买卖方向
            exchange: 交易所
            
        Returns:
            Position: 持仓数据
        """
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
        
        Returns:
            int: 下一个订单引用号
        """
        with threading.Lock():
            self.order_ref += 1
            return self.order_ref

    def __load_cache(self):
        """加载合约缓存，如果当日已缓存则直接加载"""
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
        """写入合约缓存到文件"""
        data_path = get_config('data_path')
        contracts_path = os.path.join(data_path, 'contracts.pkl')
        with open(contracts_path, 'wb') as f:
            pickle.dump({'contracts': self.acct_detail.contracts_map, 'date': datetime.today().date()}, f)
    




    

