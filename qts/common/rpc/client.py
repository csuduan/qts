import threading
import time
import socket
import uuid
from typing import  Callable

from ..queue import TTLQueue
from ..event import event_engine,Event
from ..message import Message,MsgType
from qts.common import get_logger
from .utils import *

log = get_logger(__name__)

class TcpClient:
    def __init__(self, host='127.0.0.1', port=8080, id:str = None):
        self.host = host
        self.port = port
        self.id = id

        self._socket:socket.socket = None 
        self._connected = False
        self._lock = threading.Lock()
        self.send_buffer = TTLQueue(5)

    def start(self):
        """启动客户端"""  
        log.info("start rpc client...")
        self.running = True
        with self._lock:
            self.connect()

        threading.Thread(target=self.__run, daemon=True).start()
        threading.Thread(target=self.__heartbeat, daemon=True).start()

    def connect(self):
        try:
            if self._connected:
                return          
            if self.__is_alive():
                self._socket.close()
            
            self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._socket.connect((self.host, self.port))   
            self._connected = True
            log.info(f"connect to {self.host}:{self.port} success!")
            self.__on_connect()   
            
        except Exception as e:
            if self._connected:
                log.error(f"connect to {self.host}:{self.port} error: {e}")
            self.disconnect()

    def disconnect(self):
        log.warning(f"disconnect to {self.host}:{self.port}")
        self._connected = False
        if self.__is_alive():
            self._socket.close()
            self._socket = None

    def __on_connect(self):
        #发起注册
        self.send(MsgType.REGISTER,{"id":self.id})
        #发送缓存消息
        for (type,data) in self.send_buffer.get_all():     
            self.send(type,data)
        event_engine.put(MsgType.ON_CONNECTED,{})

    
    def __on_message(self,msg):
        #添加到事件队列中
        event_engine.put(msg['type'],msg['data'])

    def __run(self):
        while self.running:
            if not self._connected:
                time.sleep(0.1)
                continue

            try:
                _,msg = recv_message(self._socket)
                try:
                    if self.__on_message:
                        self.__on_message(msg)
                except Exception as e:
                    log.error(f"handle data error: {e}")
            except Exception as e:
                log.error(f"receive data error: {e}")
                self.disconnect()

    def stop(self):
        """停止客户端"""
        self.running=False
        self.disconnect()
        
    def __is_alive(self):
        return self._socket and self._socket.fileno() != -1

    def send(self, type:str,data: any) -> bool:
        """发送消息"""        
        if not self._connected:
            self.send_buffer.put((type,data),300)
            log.warning(f"连接已断开,消息已缓存")
            return False

        msg = {
            'id': str(uuid.uuid4()),
            'type': type,
            'data': data
        }
        data = pack_message(HeadType.REQUEST, msg)      
        try:
            self._socket.sendall(data)
            return True
        except Exception as e:
            log.error(f"发送失败:{e}")
            return False
        
    def __heartbeat(self):
        """Monitor connection status"""
        while self.running:
            # 如果连接断开则自动重连
            if not self._connected:
                with self._lock:
                    self.connect()
            # 如果连接正常则发送心跳
            if self._connected:
                try:
                    self.send(MsgType.HEARTBEAT, {})
                except Exception as e:
                    log.error(f"Failed to send heartbeat: {e}")
                    self.disconnect()
            time.sleep(HEARTBEAT_INTERVAL)