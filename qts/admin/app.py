import os,sys
import json
import nest_asyncio
import asyncio
from contextlib import asynccontextmanager
from fastapi import WebSocket,Request,WebSocketDisconnect,FastAPI
from fastapi.responses import JSONResponse


from .router import sys_router,acct_router
from .core import acct_mgr,job_mgr

from qts.common import  get_config
from qts.common import  get_logger
log = get_logger(__name__)

nest_asyncio.apply()
app_name = get_config('app_name')

@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("启动前执行")
    job_mgr.start(get_config('jobs'))
    acct_mgr.start()
    yield
    log.info("关闭后前执行")

app = FastAPI(lifespan=lifespan)
app.include_router(sys_router.router,prefix=f"/{app_name}",tags=['系统管理'])
app.include_router(acct_router.router,prefix=f"/{app_name}",tags=['账户管理'])




@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    try:
        await acct_mgr.ws_mgr.connect(websocket)
        await websocket.send_json({"type": "on_connect"})
        # 发送初始账户信息
        for inst in acct_mgr.get_all_insts():
            await websocket.send_json({"type": "on_acct_detail","acct_id":inst.acct_id, "data": json.loads(inst.acct_detail.json())})
            await websocket.send_json({"type": "on_logs","acct_id":inst.acct_id, "data": inst.log_buffer})
        
        while True:
            await websocket.receive_text()
            #log.info(f"Received data: {data}")           
    except WebSocketDisconnect as e:
        acct_mgr.ws_mgr.disconnect(websocket)
    except Exception as e:
        log.error(f"WebSocket error: {str(e)}")


@app.middleware("http")
async def log_request_response(request: Request, call_next):
    # 记录请求信息
    body = await request.body()
    # 获取响应
    response = await call_next(request)   
    # 如果是 JSONResponse，记录响应内容
    if isinstance(response, JSONResponse):
        log.info(f"\n--- Response ---\n"
                   f"Status: {response.status_code}\n"
                   f"Headers: {dict(response.headers)}\n"
                   f"Body: {response.body.decode()}")  
    return response