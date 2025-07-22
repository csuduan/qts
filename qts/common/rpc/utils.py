import pickle
import struct
import socket
from typing import Tuple, Any


from enum import IntEnum

class HeadType(IntEnum):
    REQUEST = 1

HEADER_SIZE = 8  # 4 bytes type + 4 bytes length
HEARTBEAT_INTERVAL = 15  # seconds
RECONNECT_INTERVAL = 3  # seconds
REQUEST_TIMEOUT = 15.0  # seconds 

def pack_message(header_type: int , data: Any) -> bytes:
    """Pack message with header (type + length) and pickled data"""
    body = pickle.dumps(data)
    header = struct.pack('!II', header_type, len(body))
    return header + body

def unpack_header(header: bytes) -> Tuple[int, int]:
    """Unpack header to get message type and body length"""
    return struct.unpack('!II', header)

def recv_all(sock:socket.socket, n: int) -> bytes:
    """Receive exactly n bytes from socket"""
    data = bytearray()
    while len(data) < n:
        packet = sock.recv(n - len(data))
        if not packet:
            raise ConnectionError("Connection closed")
        data.extend(packet)
    return bytes(data)

def recv_message(sock: socket.socket) -> tuple[int,any]:
    """Receive message from socket"""
    header = recv_all(sock, HEADER_SIZE)
    header_type, body_length = unpack_header(header)
    # Receive body
    body_bytes = recv_all(sock, body_length)
    body = pickle.loads(body_bytes)
    return header_type,body

class RequestTimeoutError(Exception):
    """Request timeout error"""
    pass 
