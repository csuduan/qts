import os,sys
import json
import nest_asyncio
from fastapi_offline import FastAPIOffline
from fastapi import WebSocket,Request,WebSocketDisconnect
from fastapi.responses import JSONResponse

import uvicorn
from router import sys_router,acct_router
from core import acct_mgr
from config import  get_setting
from qts.tcp.server import TcpServer


nest_asyncio.apply()
app_name = get_setting('app_name')
app = FastAPIOffline()
app.include_router(sys_router.router,prefix=f"/{app_name}",tags=['系统管理'])
app.include_router(acct_router.router,prefix=f"/{app_name}",tags=['账户管理'])


from qts.log import  get_logger

log = get_logger(__name__)
acct_mgr.start()

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    log.info(f"WebSocket connection accepted")
    acct_mgr.active_websockets.append(websocket)
    try:
        # Send initial connection message
        await websocket.send_json({"type": "on_connect"})
        
        while True:
            data = await websocket.receive_text()
            #log.info(f"Received data: {data}")
               
    except WebSocketDisconnect:
        acct_mgr.active_websockets.remove(websocket)
        log.info("WebSocket connection closed")
    except Exception as e:
        acct_mgr.active_websockets.remove(websocket)
        log.error(f"WebSocket error: {str(e)}")
        raise


@app.middleware("http")
async def log_request_response(request: Request, call_next):
    # 记录请求信息
    body = await request.body()
    # log.info(f"\n--- Request ---\n"
    #             f"{request.method} {request.url}\n"
    #             f"Headers: {dict(request.headers)}\n"
    #             f"Body: {body.decode()}")
    
    # 获取响应
    response = await call_next(request)
    
    # 如果是 JSONResponse，记录响应内容
    if isinstance(response, JSONResponse):
        log.info(f"\n--- Response ---\n"
                   f"Status: {response.status_code}\n"
                   f"Headers: {dict(response.headers)}\n"
                   f"Body: {response.body.decode()}")
    
    return response



if __name__ == '__main__':
    uvicorn.run(app,host='0.0.0.0',port=get_setting('server_port'))
