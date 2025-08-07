import socket
import threading
import time
import uuid

from .utils import *
from qts.common import  get_logger
from qts.common.event import event_engine
from qts.common.message import Message, MsgType

log = get_logger(__name__)

from typing import TypeVar, Callable

T = TypeVar('T')
R = TypeVar('R')
Handler = Callable[[T], R]

class Connection:
    def __init__(self,sock:socket.socket):
        self.sock = sock
        self.id = None
        self.timestamp = time.time()
        self.on_message:Callable[[any],None] = None
    
    def update_timestamp(self):
        self.timestamp = time.time()

    def is_active(self):
        return self.sock and self.sock.fileno() != -1
    
    def  is_dead(self):
        return time.time() - self.timestamp > HEARTBEAT_INTERVAL * 4

    def close(self):
        log.warning(f"关闭连接:[{self.id}]")
        try:
            if self.is_active():
                self.sock.close()
            event_engine.put(MsgType.ON_RPC_DISCONNECTED,self)
        except:
            log.error(f"关闭连接失败")
    
    def send(self,type:str,data:any):
        if not self.sock:
            log.warning(f"连接已断开")
            return

        msg = {
            'id': str(uuid.uuid4()),
            'type': type,
            'data': data
        }
        message = pack_message(HeadType.REQUEST, msg)  
        try:
            self.sock.sendall(message)
        except Exception as e:
            log.error(f"Failed to send message: {e}")
              
class TcpServer:
    def __init__(self, host: str = "0.0.0.0", port: int = 8888):
        self._host = host
        self._port = port
        self.server_socket = None
        self._running = False
        self._lock = threading.Lock()
        self.active_conns: dict[socket.socket, Connection] = {}

    def start(self):
        """Start server"""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind((self._host, self._port))
        self.server_socket.listen(5)
        self._running = True

        threading.Thread(target=self._accept_loop, daemon=True).start()
        threading.Thread(target=self.__health_check, daemon=True).start()
        log.info(f"rpc server started on {self._host}:{self._port} !")

    def _accept_loop(self):
        """Accept new connections"""
        while self._running:
            try:
                client_socket, address = self.server_socket.accept()
                log.info(f"Client[{address}] connected")
                conn = Connection(client_socket)
                self.active_conns[client_socket] = conn
                threading.Thread(
                    target=self._handle_client,
                    args=(conn,),
                    daemon=True
                ).start()
            except Exception as e:
                if self._running:
                    log.error(f"Accept error: {e}")

    def _handle_client(self,conn:Connection):
        while self._running and conn.is_active():
            try:
                _,msg = recv_message(conn.sock)
                conn = self.active_conns[conn.sock]
                conn.update_timestamp()
                type = msg['type']
                data = msg['data']
                if type == MsgType.HEARTBEAT:
                    # 忽略心跳包
                    pass
                elif type == MsgType.REGISTER:
                    conn.id = data['id']
                    event_engine.put(MsgType.ON_RPC_CONNECTED,conn)
                else:
                    # 处理消息
                    if conn.on_message:
                        conn.on_message(msg)

            except ConnectionError as e:
                log.error(f"读取数据错误: {e}")
                break
            except Exception as e:
                log.exception(f"处理消息错误:{e}")
        log.warning(f"Client[{conn.id}] 工作线程退出!")
        self.active_conns.pop(conn.sock,None) 
        conn.close()
                
    def stop(self):
        """Stop server"""
        self._running = False
        if self.server_socket:
            self.server_socket.close()
        
        with self._lock:
            for client_socket in list(self.active_conns.keys()):
                client_socket.close()
            self.active_conns.clear()         

    def __health_check(self):
        """健康检查"""
        while self._running:
            dead_conns:list[Connection] = []    
            with self._lock:
                for conn in self.active_conns.values():
                    if conn.is_dead():
                        dead_conns.append(conn)         
            # Remove dead clients
            for conn in dead_conns:
                log.warning(f"conn heart beat timeout:[{conn.id}]")
                self.active_conns.pop(conn.sock,None)
                conn.close()           
            time.sleep(HEARTBEAT_INTERVAL)


