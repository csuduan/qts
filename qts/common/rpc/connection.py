import socket
import threading
import time
import uuid
from typing import Dict, Optional, Any, Callable
from .constants import *
from .utils import *

from qts.common import  get_logger

log = get_logger(__name__)

class Connection:
    def __init__(self, host: str, port: int,push_callback: Callable, conn_type: ConnType):
        self._host = host
        self._port = port
        self._socket = None
        self._running = False
        self._connected = False
        self._lock = threading.Lock()
        self._last_heartbeat = 0
        self._push_callback = push_callback
        self._conn_type = conn_type
        # Response handling
        self._pending_requests: Dict[str, threading.Event] = {}
        self._responses: Dict[str, Any] = {}


    def start(self):
        """Start connection"""
        self._running = True
        self._connect()
        threading.Thread(target=self._receive_loop, daemon=True).start()
        threading.Thread(target=self._monitor_loop, daemon=True).start()

    def stop(self):
        """Stop connection"""
        self._running = False
        if self._socket:
            self._socket.close()
        with self._lock:
            for event in self._pending_requests.values():
                event.set()
            self._pending_requests.clear()
            self._responses.clear()

    def reconnect(self):
        """Reconnect to server"""
        self._disconnect()
        self._connect()

    def is_connected(self) -> bool:
        """Check if connection is alive"""
        with self._lock:
            # 检查最后发送心跳的时间和连接状态
            is_alive = (self._connected and 
                       time.time() - self._last_heartbeat < HEARTBEAT_INTERVAL * 3)
            if not is_alive and self._connected:
                log.warning("Connection timeout, forcing disconnect")
                self._disconnect()
            return is_alive


    def send_request(self, msg_type: int, data: Any, timeout: float = REQUEST_TIMEOUT) -> Any:
        """Send request and wait for response"""
        if not self.is_connected():
            raise ConnectionError("Not connected to server")

        req_id = str(uuid.uuid4())
        message_data = {"req_id": req_id, "data": data}
        
        try:
            event = threading.Event()
            with self._lock:
                self._pending_requests[req_id] = event

            message = pack_message(msg_type, message_data)
            with self._lock:
                self._socket.sendall(message)

            if not event.wait(timeout):
                with self._lock:
                    self._pending_requests.pop(req_id, None)
                    self._responses.pop(req_id, None)
                raise RequestTimeoutError(f"Request timeout after {timeout} seconds")

            with self._lock:
                self._pending_requests.pop(req_id)
                response = self._responses.pop(req_id)
            return response

        except Exception as e:
            if isinstance(e, RequestTimeoutError):
                raise
            log.error(f"Send error: {e}")
            self._disconnect()  # Use _disconnect() instead of just setting flag
            raise

    def send_message(self, msg_type: int, data: Any):
        """Send message without waiting for response"""
        if not self.is_connected():
            raise ConnectionError("Not connected to server")

        try:
            message = pack_message(msg_type, data)
            with self._lock:
                self._socket.sendall(message)
        except Exception as e:
            log.error(f"Send error: {e}")
            self._disconnect() 
            raise

    def _connect(self):
        """Connect to server and send connection type"""
        try:
            if self._socket:
                self._socket.close()
            
            self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._socket.connect((self._host, self._port))
            
            if self._conn_type == ConnType.PSH:
                #发起订阅
                message = pack_message(HeadType.SUBS, {})
                self._socket.sendall(message)
            
            self._connected = True
            self._last_heartbeat = time.time()
            log.info(f"Conn[{self._conn_type.name}] connected to {self._host}:{self._port}")
            
        except Exception as e:
            if self._connected:
                log.error(f"Connect error: {e}")
            self._disconnect()
    
    def _disconnect(self):
        """Disconnect from server and clean up"""
        self._connected = False
        if self._socket:
            try:
                self._socket.close()
            except:
                pass
        with self._lock:
            # Clean up pending requests
            for event in self._pending_requests.values():
                event.set()
            self._pending_requests.clear()
            self._responses.clear()

    def _receive_loop(self):
        """Receive messages from server"""
        while self._running:
            if not self._connected:
                time.sleep(0.1)
                continue

            try:
                # Receive header
                header = recv_all(self._socket, HEADER_SIZE)
                msg_type, body_length = unpack_header(header)

                # Receive body
                body = recv_all(self._socket, body_length)
                data = pickle.loads(body)

                # 更新心跳时间(收到任何消息都更新心跳时间)
                self._last_heartbeat = time.time()
                
                if self._conn_type == ConnType.REQ:
                    # Handle response
                    if msg_type == HeadType.RESPONSE:
                        self._handle_response(data)
                        continue
                else:
                    # Handle push message
                    if self._push_callback and msg_type == HeadType.PUSH:
                        msg_id = data.get("msg_id")
                        self._push_callback(data.get("data"))

            except Exception as e:
                log.error(f"Conn[{self._conn_type.name}] receive error: {e}")
                self._connected = False

    def _handle_response(self, response_data: dict):
        """Handle response message"""
        req_id = response_data.get("req_id")
        if not req_id:
            return

        with self._lock:
            if req_id in self._pending_requests:
                self._responses[req_id] = response_data.get("data")
                self._pending_requests[req_id].set()

    def _monitor_loop(self):
        """Monitor connection status"""
        while self._running:
            # 如果连接断开则重新连接
            if not self._connected:
                self._connect()
            # 如果连接正常则发送心跳
            if self._connected:
                try:
                    self.send_message(HeadType.HEARTBEAT, time.time())
                    self._last_heartbeat = time.time()
                except Exception as e:
                    log.error(f"Failed to send heartbeat: {e}")
                    self._disconnect()
            time.sleep(HEARTBEAT_INTERVAL)