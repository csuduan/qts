from dataclasses import dataclass
from .constant import *
from pydantic import BaseModel



@dataclass
class AcctConf():
    id: str
    group: str
    name: str
    user: str
    td_addr: str
    md_addr: str
    enable: bool
    port: int
    req_address: str
    pub_address: str

@dataclass
class AcctInfo():
    id: str
    group: str
    name: str
    enable: bool = False

    conf: AcctConf = None

    trading_day: str = None
    td_status: str = False
    md_status: str = False
    balance: float = 0
    frozen: float = 0
    profit: float = 0
    margin_pct: float = 0

    
    def __post_init__(self) -> None:
        self.available: float = self.balance - self.frozen


@dataclass
class Position():
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
