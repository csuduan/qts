from fastapi import  WebSocket
from qts.common import  get_logger
import asyncio
import queue
log = get_logger(__name__)

class WsMgr:
    def __init__(self):
        self.active_connections: list[WebSocket] = []
        self.msg_queue: asyncio.Queue = asyncio.Queue()
    def start(self):
        asyncio.create_task(self.send_loop())


    # 添加新连接
    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        log.info(f"WebSocket connection accepted")
        self.active_connections.append(websocket)

    # 移除断开的连接
    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)
        log.info("WebSocket connection closed")


    # 向所有连接广播消息
    async def broadcast(self, msg: str):
        for connection in self.active_connections:
            try:
                await connection.send_json(msg)
            except Exception as e:
                log.exception(f"ws push error:{e}")
    

    async def send_loop(self):
        while True:
            try:
                msg =  await self.msg_queue.get()
                await self.broadcast(msg)
            except Exception as e:
                log.exception(f"ws push error:{e}")

    def push_msg(self, msg: str):
        self.msg_queue.put_nowait(msg)

