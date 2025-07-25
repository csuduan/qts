from datetime import datetime
from dataclasses import dataclass,field
from tkinter import N

from numpy import str_
from .constant import *
from typing import List,Dict
from pydantic import BaseModel

ACTIVE_STATUSES = set([Status.SUBMITTING, Status.NOTTRADED, Status.PARTTRADED])



class AcctConf(BaseModel):
    id: str 
    group: str 
    name: str 
    user: str 
    pwd: str 
    broker:str 
    auth:str 
    td_addr: str 
    md_addr: str 
    enable: bool 
    remark: str | None = None

class AcctInfo(BaseModel):
    conf: AcctConf = None

    id: str = None
    group: str = None
    name: str = None
    enable: bool = False
    status: bool = True  #账户状态

    trading_day: str = None
    td_status: bool = False
    md_status: bool = False
    balance: float = 0
    frozen: float = 0
    margin_rate: float = 0
    available: float = 0
    hold_profit: float = 0
    close_profit: float = 0

    timestamp: str = ""

    def model_post_init(self, __context) ->None:
        self.id = self.conf.id
        self.group = self.conf.group
        self.name = self.conf.name
        self.enable = self.conf.enable
        self.status = self.conf.enable
        self.timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
        

#@dataclass
class TickData(BaseModel):
    """
    Tick data contains information about:
        * last trade in market
        * orderbook snapshot
        * intraday market statistics.
    """

    symbol: str
    exchange: Exchange
    time: datetime 
    std_symbol: str = None

    name: str = ""
    volume: float = 0
    turnover: float = 0
    open_interest: float = 0
    last_price: float = 0
    last_volume: float = 0
    limit_up: float = 0
    limit_down: float = 0

    open_price: float = 0
    high_price: float = 0
    low_price: float = 0
    pre_close: float = 0

    bid_price_1: float = 0
    bid_price_2: float = 0
    bid_price_3: float = 0
    bid_price_4: float = 0
    bid_price_5: float = 0

    ask_price_1: float = 0
    ask_price_2: float = 0
    ask_price_3: float = 0
    ask_price_4: float = 0
    ask_price_5: float = 0

    bid_volume_1: float = 0
    bid_volume_2: float = 0
    bid_volume_3: float = 0
    bid_volume_4: float = 0
    bid_volume_5: float = 0

    ask_volume_1: float = 0
    ask_volume_2: float = 0
    ask_volume_3: float = 0
    ask_volume_4: float = 0
    ask_volume_5: float = 0


    def model_post_init(self, __context) -> None:
        """"""
        self.std_symbol: str = f"{self.symbol}.{self.exchange.value}"

class BarData(BaseModel):
    """
    Candlestick bar data of a certain trading period.
    """

    symbol: str
    exchange: Exchange
    time: datetime

    interval: Interval = None
    volume: float = 0
    turnover: float = 0
    open_interest: float = 0
    open_price: float = 0
    high_price: float = 0
    low_price: float = 0
    close_price: float = 0

    def model_post_init(self, __context) -> None:
        """"""
        self.vt_symbol: str = f"{self.symbol}.{self.exchange.value}"
    
    def is_active(self) -> bool:
        """
        Check if the order is active.
        """
        return self.status in ACTIVE_STATUSES

class Position(BaseModel):
    symbol: str
    exchange: Exchange
    direction: PosDirection

    id: str =None
    volume: int = 0
    yd_volume: int = 0
    td_volume: int =0
    yestday_volume: int =0  #昨日持仓量(静态值)
    frozen: int = 0  #>0开仓冻结，<0平仓冻结
    available: int = 0

    avg_price: float = 0 # 持仓均价
    pre_price: float = 0 # 昨日结算价
    hold_cost: float = 0 # 持仓成本(非均价)  当天新开仓按开仓价计算，昨仓则是用昨结算价计算，计算公式为price*volume*RateMultiple
    hold_profit: float = 0 # 持仓盈亏(盯市,平均成本法计算)
    close_profit: float = 0 # 平仓盈亏(盯市,平均成本法计算)
    margin: float = 0 # 保证金占用
    commission: float = 0 # 手续费

    multiple: int = 0


    def model_post_init(self, __context) -> None:
        """"""
        self.id: str = f"{self.symbol}-{self.direction.value}"


class TradeData(BaseModel):
    id: str
    symbol: str
    exchange: Exchange
    trading_day: str
    std_symbol : str = None

    direction: Direction = None
    offset: Offset = Offset.NONE
    price: float = 0
    volume: int = 0
    time: str = None
    order_ref: str = None
    fee: float = 0

    def model_post_init(self, __context) -> None:
        """"""
        self.std_symbol: str = f"{self.symbol}.{self.exchange.value}"

class OrderData(BaseModel):
    #两张组合ExchangeID + OrderSysID，FrontID + SessionID + OrderRef
    order_ref: str
    symbol: str
    exchange: Exchange = Exchange.NONE
    order_sys_id: str = None

    type: OrderType = OrderType.NOR
    direction: Direction = Direction.BUY
    offset: Offset = Offset.NONE
    price: float = 0
    volume: int = 0
    traded: int = 0
    status: Status = Status.SUBMITTING
    status_msg: str = ""
    trading_day: str = None
    time: str = None

    updatetimes: datetime = None
    deleted: bool = False

    def get_std_symbol(self) -> None:
        """"""
        return f"{self.symbol}.{self.exchange.value}"

    def is_active(self) -> bool:
        """
        Check if the order is active.
        """
        return self.status in ACTIVE_STATUSES



    

class ProductData(BaseModel):
    """
    Product data contains basic information about each product traded.
    """
    id: str
    name: str
    type: ProductType
    exchange: Exchange

    multiple: int
    pricetick: float
class ContractData(BaseModel):
    """
    Contract data contains basic information about each contract traded.
    """

    symbol: str
    exchange: Exchange
    name: str
    product: ProductType = None
    multiple: int = 0 
    pricetick: float = 0 
    vt_symbol: str = None

    min_volume: float = 1           # minimum trading volume of the contract
    stop_supported: bool = False    # whether server supports stop order
    net_position: bool = False      # whether gateway uses net position volume
    history_data: bool = False      # whether gateway provides bar history data

    option_strike: float = 0
    option_underlying: str = ""     # vt_symbol of underlying contract
    option_type: OptionType = None
    option_listed: datetime = None
    option_expiry: datetime = None
    option_portfolio: str = ""
    option_index: str = ""          # for identifying options with same strike price

    maxMarginSideAlgorithm: int =0 #大额单边保证金标准
    long_margin_by_volume: float = 0
    long_margin_by_money: float =0 
    short_margin_by_volume: float = 0
    short_margin_by_money: float = 0

    def model_post_init(self, __context)-> None:
        """"""
        self.vt_symbol: str = f"{self.symbol}.{self.exchange.value}"

class StatusData(BaseModel):
    type:str
    status:bool
    trading_day:str = None
    order_ref:int = 0


class OrderCancel(BaseModel):
    #组合1：FrontID + SessionID + OrderRef
    order_ref: str = None
    #组合2：ExchangeID + OrderSysID
    order_sys_id:str  = None
    exchange: Exchange = Exchange.NONE

class OrderRequest(BaseModel):
    symbol: str
    exchange: Exchange = Exchange.NONE
    offset: Offset = Offset.OPEN
    direction: Direction = Direction.BUY
    type: OrderType = OrderType.NOR
    volume: float = 0
    price: float = 0

class SubscribeRequest(BaseModel):
    symbols: list[str] = []
    exchange: Exchange = Exchange.NONE

class AccountData(BaseModel):
    """
    Account data contains information about balance, frozen and
    available.
    """
    accountid: str
    balance: float = 0
    available: float = 0
    frozen: float = 0

    margin_rate: float = 0
    hold_profit: float = 0
    close_profit: float = 0

class AcctDetail(BaseModel):
    acct_info: AcctInfo
    position_map: Dict[str,Position] = {}
    trade_map: Dict[str,TradeData] = {}
    order_map: Dict[str,OrderData] = {} #挂单队列
    tick_map: Dict[str,TickData] = {}
    product_map: Dict[str, ProductData] = {}
    contracts_map: Dict[str, ContractData] = {}
    

    def update(self):
        #更新账户时间戳
        self.acct_info.timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
        #更新持仓保证金
        for pos in self.position_map.values():
            contract = self.contracts_map[pos.symbol]
            #默认按昨结算计算保证金(实际存在按最新价计算的)
            if pos.direction == PosDirection.LONG:
                pos.margin = round((contract.long_margin_by_volume+contract.long_margin_by_money*contract.multiple*pos.pre_price)*(pos.volume+pos.frozen),2)
            else:
                pos.margin = round((contract.short_margin_by_volume+contract.short_margin_by_money*contract.multiple*pos.pre_price)*(pos.volume+pos.frozen),2)
        #跟新账户保证金：按照合约代码分组，根据如果合约信息中有大额单边保证金，则收取单边，否则收取双边
        pos_symbols = list(set([pos.symbol for pos in self.position_map.values()]))
        sum_margin = 0
        for symbol in pos_symbols:
            pos_list = [pos for pos in self.position_map.values() if pos.symbol == symbol]
            contract = self.contracts_map[symbol]
            if contract.maxMarginSideAlgorithm == 1:
                #大额单边保证金
                pos_long = [pos for pos in pos_list if pos.direction == PosDirection.LONG]
                pos_short = [pos for pos in pos_list if pos.direction == PosDirection.SHORT]
                margin_long = sum([pos.margin for pos in pos_long])
                margin_short = sum([pos.margin for pos in pos_short])                 
                sum_margin +=  abs(margin_long - margin_short)

            else:
                #双边保证金
                sum_margin += sum([pos.margin for pos in pos_list]) 
        self.acct_info.margin_rate = sum_margin/self.acct_info.balance if self.acct_info.balance > 0 else 0
        #TODO 更新手续费
        #更新盈亏
        sum_profit = 0
        for pos in self.position_map.values():
            last_tick = self.tick_map.get(pos.symbol,None)
            if last_tick is None:
                continue
            pos.hold_profit = round(pos.volume*pos.multiple*last_tick.last_price - pos.hold_cost,2) * (1 if pos.direction == PosDirection.LONG else -1)
            sum_profit += pos.hold_profit
        self.acct_info.hold_profit = sum_profit


