from enum import IntEnum

class MsgType(IntEnum):
    HEARTBEAT = 1
    REQUEST = 2
    RESPONSE = 3
    PUSH = 4
    CONN_TYPE = 5  # 新增：连接类型消息

class ConnType(IntEnum):
    REQ = 1  # 请求响应连接
    PSH = 2    # 推送连接

HEADER_SIZE = 8  # 4 bytes type + 4 bytes length
HEARTBEAT_INTERVAL = 5  # seconds
RECONNECT_INTERVAL = 3  # seconds
REQUEST_TIMEOUT = 15.0  # seconds 