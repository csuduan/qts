import os,sys
import nest_asyncio
from fastapi_offline import FastAPIOffline
from fastapi import WebSocket
import uvicorn
from router import sys_router,acct_router

from flask_socketio import SocketIO,emit
from utils import configs

from core import acct_mgr

app_id = configs['app_id']
app = FastAPIOffline()
app.include_router(sys_router.router,prefix=f"/{app_id}/v1/sys",tags=['系统管理'])
app.include_router(acct_router.router,prefix=f"/{app_id}/v1/acct",tags=['账户管理'])


acct_mgr.start_all()

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    while True:
        data = await websocket.receive_text()
        await websocket.send_text(f"Message received: {data}")

if __name__ == '__main__':
    uvicorn.run(app,host='0.0.0.0',port=configs['server_port'])
