from enum import Enum
from dataclasses import dataclass
from typing import TypeVar, Callable


class MsgType(Enum):
    CONNECT = "connect"
    DISCONNECT = "disconnect"
    CLOSE = "close"
    GET_POSITIONS = "get_positions"
    GET_TRADES = "get_trades"
    GET_ORDERS = "get_orders"
    GET_ACCT_INFO = "get_acct_info"
    GET_QUOTES = "get_quotes"
    GET_ACCT_DETAIL = "get_acct_detail"
    SEND_ORDER = "send_order"
    CANCEL_ORDER = "cancel_order"

    ON_TICK = "on_tick"
    ON_LOG = "on_log"
    ON_ORDER = "on_order"
    ON_POSITION = "on_position"
    ON_TRADE = "on_trade"
    ON_ACCT_INFO = "on_acct_info"
    ON_READY = "on_ready"
    
@dataclass
class Message():
    type: MsgType
    code: int = 0 ;
    data: any = None;

R = TypeVar('R')  # 返回值类型
A1 = TypeVar('A1')  # 第一个参数类型
Handler = Callable[[A1], R]

class MsgHandler:
    def __init__(self):
        self._handlers: dict[MsgType|str, Handler] = {}

    def register(self, msg_type: MsgType|str):
        def wrapper(func: Handler):
            self._handlers[msg_type] = func
            return func
        return wrapper

    def get_handler(self, msg_type: MsgType|str)->Handler:
        if msg_type not in self._handlers:
            return None
        return self._handlers.get(msg_type)