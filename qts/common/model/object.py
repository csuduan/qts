from datetime import datetime
from dataclasses import dataclass,field
from .constant import *
from typing import List,Dict
from pydantic import BaseModel

ACTIVE_STATUSES = set([Status.SUBMITTING, Status.NOTTRADED, Status.PARTTRADED])



class AcctConf(BaseModel):
    id: str
    group: str
    name: str
    user: str
    broker:str
    auth:str
    td_addr: str
    md_addr: str
    enable: bool
    req_address: str
    pub_address: str
    product_info: str= "qts"
    tcp_port: int = 6000


class AcctInfo(BaseModel):
    id: str
    group: str
    name: str
    enable: bool = False
    status: bool = True  #账户状态

    conf: AcctConf = None

    trading_day: str = None
    td_status: bool = False
    md_status: bool = False
    balance: float = 0
    frozen: float = 0
    margin_rate: float = 0
    available: float = 0
    hold_profit: float = 0
    close_profit: float = 0





#@dataclass
class Position(BaseModel):
    id: str  #symbol-direction
    symbol: str
    exchange: Exchange
    direction: PosDirection

    volume: int = 0
    yd_volume: int = 0
    td_volume: int =0
    yestday_volume: int =0  #昨日持仓量
    frozen: int = 0  #>0开仓冻结，<0平仓冻结
    available: int = 0



    price: float = 0 # 持仓均价
    pre_price: float = 0 # 昨日结算价
    hold_cost: float = 0 # 持仓成本
    hold_profit: float = 0 # 持仓盈亏(盯市)
    close_profit: float = 0 # 平仓盈亏(盯市)
    margin: float = 0 # 保证金占用
    commission: float = 0 # 手续费




class TradeData(BaseModel):
    id: str
    symbol: str
    exchange: Exchange
    trading_day: str

    direction: Direction = None
    offset: Offset = Offset.NONE
    price: float = 0
    volume: int = 0
    time: str = None
    order_ref: str = None
    fee: float = 0


    def __post_init__(self) -> None:
        """"""
        self.std_symbol: str = f"{self.symbol}.{self.exchange.value}"



class OrderData(BaseModel):
    #两张组合ExchangeID + OrderSysID，FrontID + SessionID + OrderRef
    order_ref: str
    symbol: str
    exchange: Exchange
    order_sys_id: str

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

    localtime: str = None

    def __post_init__(self) -> None:
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

    def __post_init__(self) -> None:
        """"""
        self.vt_symbol: str = f"{self.symbol}.{self.exchange.value}"
    
    def is_active(self) -> bool:
        """
        Check if the order is active.
        """
        return self.status in ACTIVE_STATUSES
    

class ProductData(BaseModel):
    """
    Product data contains basic information about each product traded.
    """

    name: str
    product: Product
    exchange: Exchange

    multiple: int
    pricetick: float
#@dataclass
class ContractData(BaseModel):
    """
    Contract data contains basic information about each contract traded.
    """

    symbol: str
    exchange: Exchange
    name: str
    product: Product
    multiple: int
    pricetick: float

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

    def __post_init__(self) -> None:
        """"""
        self.vt_symbol: str = f"{self.symbol}.{self.exchange.value}"

class StatusData(BaseModel):
    type:str
    status:bool
    trading_day:str = None
    order_ref:int = 0


class OrderCancel(BaseModel):
    order_ref: str
    symbol: str
    exchange: Exchange

class OrderRequest(BaseModel):
    symbol: str
    exchange: Exchange = Exchange.NONE
    offset: Offset = Offset.OPEN
    direction: Direction = Direction.BUY
    type: OrderType = OrderType.NOR
    volume: float = 0
    price: float = 0

class SubscribeRequest(BaseModel):
    symbol: str
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
    contracts_map: Dict[str, ContractData] = {}
    timestamp: str = ""
