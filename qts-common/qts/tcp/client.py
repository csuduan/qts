from typing import Optional, Dict, Set, Any
import threading
import time
from .connection import Connection
from .constants import *
from .utils import RequestTimeoutError

from typing import TypeVar, Callable



from qts.log import get_logger

log = get_logger(__name__)

class TcpClient:
    def __init__(self, host: str = "localhost", port: int = 8888, 
                 handler: Callable = None):
        self._host = host
        self._port = port
        self._cust_handler: Callable = handler
        
        # Two dedicated connections
        self._req_conn: Optional[Connection] = None  # For request-response
        self._push_conn: Optional[Connection] = None     # For server push
        
        self._message_lock = threading.Lock()
        self._message_cleanup_time = 300
        self._last_cleanup = time.time()

    def start(self):
        """Start client with two dedicated connections"""  
        # Initialize request-response connection
        self._req_conn = Connection(self._host, self._port, None,ConnType.REQ)
        self._req_conn.start()

        # Initialize push connection
        self._push_conn = Connection(self._host, self._port, self._handle_push_message,ConnType.PSH)
        self._push_conn.start()
        
 

        
        log.info("Client started with dual connections")

    def stop(self):
        """Stop both connections"""
        if self._req_conn:
            self._req_conn.stop()
        if self._push_conn:
            self._push_conn.stop()

    def request(self, req: Any, timeout: float = REQUEST_TIMEOUT) -> Any:
        """Send request and wait for response using request connection"""
        if not self._req_conn or not self._req_conn.is_connected():
            raise ConnectionError("Request connection not available")
        
        try:
            rsp = self._req_conn.send_request(MsgType.REQUEST, req, timeout)
            log.info(f"Request: {req} response: {rsp}")
            return rsp
        except Exception as e:
            log.error(f"Request failed: {e}")
            raise

    def send(self, data: Any):
        """Send message without waiting for response"""
        if not self._req_conn or not self._req_conn.is_connected():
            raise ConnectionError("Request connection not available")
        
        try:
            self._req_conn.send_message(MsgType.REQUEST, data)
        except Exception as e:
            log.error(f"Send failed: {e}")
            raise

    def _connect_handler(self):
        """Handle connect message from push connection"""
        pass

    def _handle_push_message(self, data: Any):
        """Handle push message from push connection"""
        if self._cust_handler:
            try:
                self._cust_handler(data)
            except Exception as e:
                log.error(f"Error handling push message: {e}")