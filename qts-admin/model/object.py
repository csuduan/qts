import datetime
from dataclasses import dataclass, field
from typing import List

from .constant import *

from qts.model.object import AcctConf,AcctInfo,Position,TradeData,OrderData

@dataclass
class AcctDetail:
    acct_info: AcctInfo
    positions: List[Position] = field(default_factory=list)
    trades: List[TradeData] = field(default_factory=list)
    orders: List[OrderData] = field(default_factory=list)
    timestamp: str = ""

