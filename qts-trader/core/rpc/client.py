import threading
from time import time
from typing import Any, Callable, Dict

import zmq

from utils import get_logger
from .common import TopicType, HEARTBEAT_TOLERANCE, MsgType, RpcHandler, MsgHandler

log = get_logger(__name__)


class RemoteException(Exception):
    """
    RPC remote exception
    """

    def __init__(self, value: Any) -> None:
        """
        Constructor
        """
        self._value = value

    def __str__(self) -> str:
        """
        Output error message
        """
        return self._value


class RpcClient:
    """"""

    def __init__(self) -> None:
        """Constructor"""
        # zmq port related
        self._context: zmq.Context = zmq.Context()

        # Request socket (Request–reply pattern)
        self._socket_req: zmq.Socket = self._context.socket(zmq.REQ)

        # Subscribe socket (Publish–subscribe pattern)
        self._socket_sub: zmq.Socket = self._context.socket(zmq.SUB)

        # Set socket option to keepalive
        for socket in [self._socket_req, self._socket_sub]:
            socket.setsockopt(zmq.TCP_KEEPALIVE, 1)
            socket.setsockopt(zmq.TCP_KEEPALIVE_IDLE, 60)

        # Worker thread relate, used to process data pushed from server
        self._active: bool = False  # RpcClient status
        self._thread: threading.Thread = None  # RpcClient thread
        self._lock: threading.Lock = threading.Lock()

        self._last_received_ping: time = time()

        self.sub_handler: RpcHandler = None

    def register_handler(self, handler: RpcHandler):
        self.sub_handler = handler

    def req(self, msg_type: MsgType, data: Any, timeout=3000) -> Any:
        if data is None:
            data = {}
        req: list = [msg_type, data]

        # Send request and wait for response
        with self._lock:
            self._socket_req.send_pyobj(req)

            # Timeout reached without any data
            n: int = self._socket_req.poll(timeout)
            if not n:
                msg: str = f"Timeout of {timeout}ms reached for {req}"
                raise RemoteException(msg)

            rep = self._socket_req.recv_pyobj()
            code, msg, data = rep

        # Return response if successed; Trigger exception if failed
        if code == 0:
            return data
        else:
            raise RemoteException(msg)

    def start(
            self,
            req_address: str,
            sub_address: str
    ) -> None:
        """
        Start RpcClient
        """
        if self._active:
            return

        log.info(f"start client, req_address:{req_address}, sub_address:{sub_address}")
        # Connect zmq port
        self._socket_req.connect(req_address)
        self._socket_sub.connect(sub_address)

        self._socket_sub.setsockopt_string(zmq.SUBSCRIBE, '')

        # Start RpcClient status
        self._active = True

        # Start RpcClient thread
        self._thread = threading.Thread(target=self.run)
        self._thread.start()

        self._last_received_ping = time()

    def stop(self) -> None:
        """
        Stop RpcClient
        """
        if not self._active:
            return

        # Stop RpcClient status
        self._active = False

    def join(self) -> None:
        # Wait for RpcClient thread to exit
        if self._thread and self._thread.is_alive():
            self._thread.join()
        self._thread = None

    def run(self) -> None:
        """
        Run RpcClient function
        """
        pull_tolerance: int = HEARTBEAT_TOLERANCE * 1000

        while self._active:
            if not self._socket_sub.poll(pull_tolerance):
                self.on_disconnected()
                continue

            # Receive data from subscribe socket
            topic, data = self._socket_sub.recv_pyobj(flags=zmq.NOBLOCK)

            if topic == TopicType.HEARTBEAT_TOPIC:
                self._last_received_ping = data
            else:
                # Process data by callable function
                func: MsgHandler = self.sub_handler.get_handler(topic)
                if func is None:
                    continue
                func(data)

        # Close socket
        self._socket_req.close()
        self._socket_sub.close()

    def on_disconnected(self):
        """
        Callback when heartbeat is lost.
        """
        msg: str = f"RpcServer has no response over {HEARTBEAT_TOLERANCE} seconds, please check you connection."
        print(msg)
