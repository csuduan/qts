import threading
from time import time
from typing import Any
from apscheduler.schedulers.background import BackgroundScheduler

from utils import get_logger

import zmq

from .common import HEARTBEAT_INTERVAL, RpcHandler, MsgHandler

log = get_logger(__name__)

class RpcServer:
    """"""

    def __init__(self) -> None:
        """
        Constructor
        """
        # Zmq port related
        self._context: zmq.Context = zmq.Context()

        # Reply socket (Request–reply pattern)
        self._socket_rep: zmq.Socket = self._context.socket(zmq.REP)

        # Publish socket (Publish–subscribe pattern)
        self._socket_pub: zmq.Socket = self._context.socket(zmq.PUB)

        # Worker thread related
        self._active: bool = False  # RpcServer status
        self._thread: threading.Thread = None  # RpcServer thread
        self._lock: threading.Lock = threading.Lock()

        # Heartbeat related
        self._heartbeat_at: int = None

        self.scheduler = None

        self.req_handler:RpcHandler = None


    def is_active(self) -> bool:
        """"""
        return self._active

    def register_handler(self, handler: RpcHandler):
        self.req_handler = handler

    def start(
            self,
            rep_address: str,
            pub_address: str,
    ) -> None:
        """
        Start RpcServer
        """
        if self._active:
            return

        # Bind socket address
        self._socket_rep.bind(rep_address)
        self._socket_pub.bind(pub_address)

        # Start RpcServer status
        self._active = True

        # Start RpcServer thread
        self._thread = threading.Thread(target=self.run)
        self._thread.start()

        # Init heartbeat publish timestamp
        self._heartbeat_at = time() + HEARTBEAT_INTERVAL

        self.scheduler = BackgroundScheduler()
        self.scheduler.add_job(self.check_heartbeat, 'interval', seconds=5)
        self.scheduler.start()


    def stop(self) -> None:
        """
        Stop RpcServer
        """
        if not self._active:
            return

        # Stop RpcServer status
        self._active = False

    def join(self) -> None:
        # Wait for RpcServer thread to exit
        if self._thread and self._thread.is_alive():
            self._thread.join()
        self._thread = None

    def run(self) -> None:
        """
        Run RpcServer functions
        """
        while self._active:
            # Poll response socket for 1 second
            n: int = self._socket_rep.poll(1000)
            self.check_heartbeat()

            if not n:
                continue

            if self.req_handler is None:
                continue

            # Receive request data from Reply socket
            req = self._socket_rep.recv_pyobj()

            # Get function name and parameters
            msg_type, data = req

            # Try to get and execute callable function object; capture exception information if it fails
            try:
                func: MsgHandler = self.req_handler.get_handler(msg_type)
                if func is None:
                    rep: list = [9999, f"no handler for {msg_type}", None]
                    log.error("no handler for %s", msg_type)
                    continue

                r: Any = func(data)
                rep: list = [0, "success", r]
            except Exception as e:  # noqa
                rep: list = [9999, e, None]
                log.error("process request error: %s", e)


            # send callable response by Reply socket
            self._socket_rep.send_pyobj(rep)

        # Unbind socket address
        self._socket_pub.unbind(self._socket_pub.LAST_ENDPOINT)
        self._socket_rep.unbind(self._socket_rep.LAST_ENDPOINT)

    def publish(self, topic, data: Any) -> None:
        """
        Publish data
        """
        with self._lock:
            self._socket_pub.send_pyobj([topic, data])




    def check_heartbeat(self) -> None:
        """
        Check whether it is required to send heartbeat.
        """
        now: float = time()
        if now >= self._heartbeat_at:
            # Publish heartbeat
            self.publish(TopicType.HEARTBEAT_TOPIC, now)

            # Update timestamp of next publish
            self._heartbeat_at = now + HEARTBEAT_INTERVAL
