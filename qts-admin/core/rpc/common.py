import signal
from enum import Enum
from typing import TypeVar, Callable

# Achieve Ctrl-c interrupt recv
signal.signal(signal.SIGINT, signal.SIG_DFL)

HEARTBEAT_INTERVAL = 10
HEARTBEAT_TOLERANCE = 30


class MsgType(Enum):
    CONNECT = "connect"
    DISCONNECT = "disconnect"
    CLOSE = "close"
    GET_POSITIONS = "get_positions"
    GET_TRADES = "get_trades"
    GET_ORDERS = "get_orders"
    GET_ACCT_INFO = "get_acct_info"
    GET_ACCT_DETAIL = "get_acct_detail"
    SEND_ORDER = "send_order"
    CANCEL_ORDER = "cancel_order"

    HEARTBEAT_TOPIC = "heartbeat"
    ON_TICK = "on_tick"
    ON_LOG = "on_log"
    ON_ORDER = "on_order"
    ON_POSITION = "on_position"
    ON_TRADE = "on_trade"
    ON_ACCT_INFO = "on_acct_info"



R = TypeVar('R')  # 返回值类型
A1 = TypeVar('A1')  # 第一个参数类型

MsgHandler = Callable[[A1], R]


class RpcHandler:
    def __init__(self):
        self._handlers: dict[MsgType, MsgHandler] = {}

    def register_handler(self, msg_type: MsgType):
        def wrapper(func: MsgHandler):
            self._handlers[msg_type] = func
        return wrapper

    def get_handler(self, msg_type: MsgType):
        if msg_type not in self._handlers:
            return None
        return self._handlers.get(msg_type)
