from enum import IntEnum

class HeadType(IntEnum):
    HEARTBEAT = 1
    REQUEST = 2
    RESPONSE = 3
    PUSH = 4
    SUBS = 5

class ConnType(IntEnum):
    REQ = 1  # 请求响应连接
    PSH = 2    # 推送连接

HEADER_SIZE = 8  # 4 bytes type + 4 bytes length
HEARTBEAT_INTERVAL = 5  # seconds
RECONNECT_INTERVAL = 3  # seconds
REQUEST_TIMEOUT = 15.0  # seconds 