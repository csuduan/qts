from core.rpc import RpcServer
from utils.logger import logger_utils
from .admin_handler import handler

log = logger_utils.get_logger(__name__)


class AdminEngine:
    def __init__(self):
        self.pub_addr = None
        self.rep_addr = None
        self.rpc_server = None

    def start(self, rep_addr: str, pub_addr: str):
        self.rep_addr = rep_addr
        self.pub_addr = pub_addr
        log.info(f"start admin engine, rep_addr:{self.rep_addr}, pub_addr:{self.pub_addr}")

        self.rpc_server = RpcServer()
        self.rpc_server.start(self.rep_addr, self.pub_addr)
        self.rpc_server.register_handler(handler)

    def stop(self, req) -> None:
        self.rpc_server.stop()
