import pickle
import struct
from typing import Tuple, Any

def pack_message(msg_type: int, data: Any) -> bytes:
    """Pack message with header (type + length) and pickled data"""
    body = pickle.dumps(data)
    header = struct.pack('!II', msg_type, len(body))
    return header + body

def unpack_header(header: bytes) -> Tuple[int, int]:
    """Unpack header to get message type and body length"""
    return struct.unpack('!II', header)

def recv_all(sock, n: int) -> bytes:
    """Receive exactly n bytes from socket"""
    data = bytearray()
    while len(data) < n:
        packet = sock.recv(n - len(data))
        if not packet:
            raise ConnectionError("Connection closed")
        data.extend(packet)
    return bytes(data)

class RequestTimeoutError(Exception):
    """Request timeout error"""
    pass 