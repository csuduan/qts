import os,sys
import nest_asyncio
from fastapi_offline import FastAPIOffline
from fastapi import WebSocket
import uvicorn
from router import sys_router,acct_router
from core.acct import acct_mgr
from config import  get_setting

app_id = get_setting('app_id')
app = FastAPIOffline()
app.include_router(sys_router.router,prefix=f"/{app_id}",tags=['系统管理'])
app.include_router(acct_router.router,prefix=f"/{app_id}",tags=['账户管理'])


acct_mgr.start()

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    while True:
        data = await websocket.receive_text()
        await websocket.send_text(f"Message received: {data}")

if __name__ == '__main__':
    uvicorn.run(app,host='0.0.0.0',port=get_setting('server_port'))
