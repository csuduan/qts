"""
General constant enums used in the trading platform.
"""
import json
from enum import Enum
from qts.common.tcp.constants import MsgType

class EnumEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Enum):
            return obj.value  # 默认返回枚举的值
        return super().default(obj)

def _(str)->str:
    return str

class Direction(Enum):
    """
    Direction of order/trade/position.
    """
    BUY = _("BUY")
    SELL = _("SELL")

class PosDirection(Enum):
    """
    Direction of order/trade/position.
    """
    LONG = _("LONG")
    SHORT = _("SHORT")
    NET = _("NET")

class Offset(Enum):
    """
    Offset of order/trade.
    """
    NONE = ""
    OPEN = _("OPEN")
    CLOSE = _("CLOSE")
    CLOSETODAY = _("CLOSETODAY")
    CLOSEYESTERDAY = _("CLOSEYESTERDAY")


class Status(Enum):
    """
    Order status.
    """
    SUBMITTING = _("提交中")
    NOTTRADED = _("未成交")
    PARTTRADED = _("部分成交")
    ALLTRADED = _("全部成交")
    CANCELLED = _("已撤销")
    REJECTED = _("错单")

class Product(Enum):
    """
    Product class.
    """
    EQUITY = _("股票")
    FUTURES = _("期货")
    OPTION = _("期权")
    INDEX = _("指数")
    FOREX = _("外汇")
    SPOT = _("现货")
    ETF = "ETF"
    BOND = _("债券")
    WARRANT = _("权证")
    SPREAD = _("价差")
    FUND = _("基金")
    CFD = "CFD"
    SWAP = _("互换")


class OrderType(Enum):
    """
    Order type.
    """
    NOR = "Nor"
    FAK = "FAK"
    FOK = "FOK"
    RFQ = _("询价")


class OptionType(Enum):
    """
    Option type.
    """
    CALL = _("看涨期权")
    PUT = _("看跌期权")


class Exchange(Enum):
    """
    Exchange.
    """
    # Chinese
    CFFEX = "CFFEX"         # China Financial Futures Exchange
    SHFE = "SHFE"           # Shanghai Futures Exchange
    CZCE = "CZCE"           # Zhengzhou Commodity Exchange
    DCE = "DCE"             # Dalian Commodity Exchange
    INE = "INE"             # Shanghai International Energy Exchange
    GFEX = "GFEX"           # Guangzhou Futures Exchange
    SSE = "SSE"             # Shanghai Stock Exchange
    SZSE = "SZSE"           # Shenzhen Stock Exchange
    BSE = "BSE"             # Beijing Stock Exchange
    SHHK = "SHHK"           # Shanghai-HK Stock Connect
    SZHK = "SZHK"           # Shenzhen-HK Stock Connect
    SGE = "SGE"             # Shanghai Gold Exchange
    WXE = "WXE"             # Wuxi Steel Exchange
    CFETS = "CFETS"         # CFETS Bond Market Maker Trading System
    XBOND = "XBOND"         # CFETS X-Bond Anonymous Trading System

    # Global
    SMART = "SMART"         # Smart Router for US stocks
    NYSE = "NYSE"           # New York Stock Exchnage
    NASDAQ = "NASDAQ"       # Nasdaq Exchange
    ARCA = "ARCA"           # ARCA Exchange
    EDGEA = "EDGEA"         # Direct Edge Exchange
    ISLAND = "ISLAND"       # Nasdaq Island ECN
    BATS = "BATS"           # Bats Global Markets
    IEX = "IEX"             # The Investors Exchange
    AMEX = "AMEX"           # American Stock Exchange
    TSE = "TSE"             # Toronto Stock Exchange
    NYMEX = "NYMEX"         # New York Mercantile Exchange
    COMEX = "COMEX"         # COMEX of CME
    GLOBEX = "GLOBEX"       # Globex of CME
    IDEALPRO = "IDEALPRO"   # Forex ECN of Interactive Brokers
    CME = "CME"             # Chicago Mercantile Exchange
    ICE = "ICE"             # Intercontinental Exchange
    SEHK = "SEHK"           # Stock Exchange of Hong Kong
    HKFE = "HKFE"           # Hong Kong Futures Exchange
    SGX = "SGX"             # Singapore Global Exchange
    CBOT = "CBT"            # Chicago Board of Trade
    CBOE = "CBOE"           # Chicago Board Options Exchange
    CFE = "CFE"             # CBOE Futures Exchange
    DME = "DME"             # Dubai Mercantile Exchange
    EUREX = "EUX"           # Eurex Exchange
    APEX = "APEX"           # Asia Pacific Exchange
    LME = "LME"             # London Metal Exchange
    BMD = "BMD"             # Bursa Malaysia Derivatives
    TOCOM = "TOCOM"         # Tokyo Commodity Exchange
    EUNX = "EUNX"           # Euronext Exchange
    KRX = "KRX"             # Korean Exchange
    OTC = "OTC"             # OTC Product (Forex/CFD/Pink Sheet Equity)
    IBKRATS = "IBKRATS"     # Paper Trading Exchange of IB

    # Special Function
    LOCAL = "LOCAL" 
    NONE = ""        # For local generated data


class Currency(Enum):
    """
    Currency.
    """
    USD = "USD"
    HKD = "HKD"
    CNY = "CNY"
    CAD = "CAD"


class Interval(Enum):
    """
    Interval of bar data.
    """
    MINUTE = "1m"
    HOUR = "1h"
    DAILY = "d"
    WEEKLY = "w"
    TICK = "tick"


class AcctOpType(Enum):
    CONNECT = "connect"
    DISCONNECT = "disconnect"

class MessageType(Enum):
    REQ_ACCT = 4,
    PUSH_ACCT = 5,
    REQ_ORDER = 6,
    PUSH_ORDER = 7,
    REQ_TRADE = 8,
    PUSH_TRADE = 9,
    