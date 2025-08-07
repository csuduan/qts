from enum import Enum
from dataclasses import dataclass
from typing import TypeVar, Callable


class MsgType(Enum):
    HEARTBEAT = "heartbeat"
    REGISTER = "register"

    REQ_CONNECT = "req_connect"
    REQ_DISCONNECT = "req_disconnect"
    REQ_CLOSE = "req_close"
    REQ_SEND_ORDER = "req_insert_order"
    REQ_CANCEL_ORDER = "req_cancel_order"
    REQ_SUBSCRIBE = "req_subscribe"

    ON_RPC_CONNECTED = "on_connected"
    ON_RPC_DISCONNECTED = "on_disconnected"
    ON_READY = "on_ready"
    ON_STATUS = "on_status"
    ON_TICK = "on_tick"
    ON_ORDER = "on_order"
    ON_POSITION = "on_position"
    ON_POSITIONS = "on_positions"
    ON_TRADE = "on_trade"
    ON_TRADES = "on_trades"
    ON_ACCT_INFO = "on_acct_info"
    ON_ACCT_DETAIL = "on_acct_detail"
    ON_CONTRACTS = "on_contracts"
    ON_LOG = "on_log"


    ON_WS = "on_ws"
    
@dataclass
class Message():
    type: MsgType
    id: str = None
    data: any = None


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