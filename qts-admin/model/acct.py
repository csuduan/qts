from dataclasses import dataclass
from .constant import *

@dataclass
class AcctInfo(object):
    id: str
    group: str
    name: str

    trading_day: str
    td_status: str
    md_status: str
    balance: float
    frozen: float
    profit: float

    def __post_init__(self) -> None:
        self.available: float = self.balance - self.frozen

@dataclass
class AcctConf(object):
    id: str
    group: str
    name: str
    user: str
    td_addr: str
    md_addr: str
    enable: bool

@dataclass
class Position(object):
    id: str
    symbol: str
    exchange: Exchange
    direction: Direction

    volume: int = 0
    yd_volume: int = 0
    td_volume: int =0
    frozen: int = 0

    price: float = 0
    pnl: float = 0
    mv: float = 0


    def __post_init__(self) -> None:
        """"""
        self.std_symbol: str = f"{self.symbol}.{self.exchange.value}"
        self.id: str = f"{self.std_symbol}-{self.direction.value}"
