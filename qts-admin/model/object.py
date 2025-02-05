import datetime
from dataclasses import dataclass
from typing import List

from .constant import *


@dataclass
class TradeData(object):
    symbol: str
    exchange: Exchange
    orderid: str
    tradeid: str
    direction: Direction = None

    offset: Offset = Offset.NONE
    price: float = 0
    volume: float = 0
    datetime: datetime = None

    def __post_init__(self) -> None:
        """"""
        self.std_symbol: str = f"{self.symbol}.{self.exchange.value}"


@dataclass
class PositionData(object):
    symbol: str
    exchange: Exchange
    direction: Direction

    volume: float = 0
    frozen: float = 0
    price: float = 0
    pnl: float = 0
    yd_volume: float = 0

    def __post_init__(self) -> None:
        """"""
        self.std_symbol: str = f"{self.symbol}.{self.exchange.value}"
        self.positionid: str = f"{self.std_symbol}-{self.direction.value}"


@dataclass
class OrderData(object):
    orderid: str

    symbol: str
    exchange: Exchange

    type: OrderType = OrderType.LIMIT
    direction: Direction = None
    offset: Offset = Offset.NONE
    price: float = 0
    volume: float = 0
    traded: float = 0
    status: Status = Status.SUBMITTING
    datetime: datetime = None
    reference: str = ""

    def __post_init__(self) -> None:
        """"""
        self.vt_symbol: str = f"{self.symbol}.{self.exchange.value}"


@dataclass
class AcctInfo(object):
    id: str
    name: str
    type: str
    td_status: str
    md_status: str

    balance: float
    frozen: float

    def __post_init__(self) -> None:
        self.available: float = self.balance - self.frozen


@dataclass
class AcctDetail(AcctInfo):
    positions: List[PositionData]
    trades: List[TradeData]
    orders: List[OrderData]
