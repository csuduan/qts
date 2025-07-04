import socket
import threading
import time
import uuid
from typing import Dict, List, Any
from .constants import *
from .utils import *
from qts.common.log import  get_logger

log = get_logger(__name__)

from typing import TypeVar, Callable

T = TypeVar('T')
R = TypeVar('R')

Handler = Callable[[T], R]


class TcpServer:
    def __init__(self, host: str = "0.0.0.0", port: int = 8888, req_handler: Handler = None,new_client_callback: Callable = None):
        self._host = host
        self._port = port
        self._socket = None
        self._req_handler = req_handler
        self._new_client_callback = new_client_callback
        self._running = False
        self._lock = threading.Lock()
        
        # 分别存储不同类型的客户端连接
        self._request_clients: Dict[socket.socket, threading.Thread] = {}
        self._push_clients: Dict[socket.socket, threading.Thread] = {}

        self._client_heartbeats = {}  # 新增：记录客户端最后心跳时间


    def start(self):
        """Start server"""
        self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self._socket.bind((self._host, self._port))
        self._socket.listen(5)
        self._running = True

        # Start accept thread
        threading.Thread(target=self._accept_loop, daemon=True).start()
        # Start heartbeat thread
        threading.Thread(target=self._monitor_clients, daemon=True).start()

        log.info(f"Server started on {self._host}:{self._port}")

    def stop(self):
        """Stop server"""
        self._running = False
        if self._socket:
            self._socket.close()
        
        with self._lock:
            for client_socket in list(self._request_clients.keys()):
                client_socket.close()
            for client_socket in list(self._push_clients.keys()):
                client_socket.close()
            self._request_clients.clear()
            self._push_clients.clear()

    def push(self, data: Any):
        """Only send push messages to push connections"""
        self.__broadcast_to_push_clients(MsgType.PUSH, data)

    def __broadcast_to_push_clients(self, msg_type: int, data: Any):
        """Broadcast message only to push clients"""
        message_data = {
            "msg_id": str(uuid.uuid4()),
            "data": data
        }
        message = pack_message(msg_type, message_data)
        
        with self._lock:
            for client_socket in list(self._push_clients.keys()):
                try:
                    client_socket.sendall(message)
                except Exception as e:
                    log.error(f"Failed to broadcast to push client: {e}")
                    self._remove_client(client_socket)

    def _accept_loop(self):
        """Accept new connections"""
        while self._running:
            try:
                client_socket, address = self._socket.accept()
                log.info(f"Client[{address}] connected")
                client_thread = threading.Thread(
                    target=self._handle_client,
                    args=(client_socket,),
                    daemon=True
                )
                client_thread.start()
            except Exception as e:
                if self._running:
                    log.error(f"Accept error: {e}")

    def _handle_client(self, client_socket: socket.socket):
        """Handle client connection"""
        conn_type = None
        peer_name = client_socket.getpeername()
        try:
            # 首先等待接收连接类型
            header = recv_all(client_socket, HEADER_SIZE)
            msg_type, body_length = unpack_header(header)
            
            if msg_type == MsgType.CONN_TYPE:
                body = recv_all(client_socket, body_length)
                data = pickle.loads(body)
                conn_type = data.get("conn_type")
                
                # 根据连接类型存储客户端
                with self._lock:
                    if conn_type == ConnType.PSH:
                        self._push_clients[client_socket] = threading.current_thread()
                    else:
                        self._request_clients[client_socket] = threading.current_thread()
                
                log.info(f"Client[{peer_name}] registered as {ConnType(conn_type).name} connection")

            if self._new_client_callback and conn_type == ConnType.PSH:
                self._new_client_callback()
            
            with self._lock:
                self._client_heartbeats[client_socket] = time.time()
            # 继续处理常规消息
            while self._running:
                try:

                    header = recv_all(client_socket, HEADER_SIZE)
                    msg_type, body_length = unpack_header(header)
                    body = recv_all(client_socket, body_length)
                    req_data = pickle.loads(body)
                    # 更新心跳时间
                    if msg_type == MsgType.HEARTBEAT:
                        with self._lock:
                            self._client_heartbeats[client_socket] = time.time()
                        continue
                                    
                    # 处理请求
                    if conn_type == ConnType.REQ:
                        req_id = req_data.get("req_id")
                        req_data = req_data.get("data")  
                        rsp = self._req_handler(req_data)
                        if rsp:
                            response_data = {
                                "req_id": req_id,
                                "data": rsp
                            }
                            response_msg = pack_message(MsgType.RESPONSE, response_data)
                            client_socket.sendall(response_msg)
                    # Push连接只处理心跳
                    elif conn_type == ConnType.PSH :
                        #Push连接不处理任何请求
                        pass
                except ConnectionError as e:
                    #连接断开则需要跳出循环
                    log.info(f"Client[{peer_name}] disconnected:  {e}")
                    break
                except Exception as e:
                    log.error(f"Error handling message: {e}")
                    break
                
        except Exception as e:
            log.error(f"Client[{peer_name}] handler error: {e}")
        finally:
            log.info(f"Client thread[{peer_name}] stopping...")
            self._remove_client(client_socket)


    def _remove_client(self, client_socket: socket.socket):
        """Remove client from appropriate client dict"""
        with self._lock:
            self._request_clients.pop(client_socket, None)
            self._push_clients.pop(client_socket, None)
            self._client_heartbeats.pop(client_socket, None)  # 清理心跳记录

        try:
            client_socket.close()
        except:
            pass
    
    def _monitor_clients(self):
        """Monitor client connections and remove dead ones"""
        while self._running:
            current_time = time.time()
            dead_clients = []
            
            with self._lock:
                for client_socket, last_heartbeat in self._client_heartbeats.items():
                    if current_time - last_heartbeat > HEARTBEAT_INTERVAL * 4:
                        dead_clients.append(client_socket)
                        pass
            
            # Remove dead clients
            for client_socket in dead_clients:
                log.warning(f"Client {client_socket.getpeername()} heartbeat timeout")
                self._remove_client(client_socket)
            
            time.sleep(HEARTBEAT_INTERVAL)

