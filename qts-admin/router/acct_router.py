from fastapi import APIRouter,Request
from pydantic import BaseModel,Extra
from utils import log_utils
from common.resp import  resp_success


from core import acct_mgr

router = APIRouter()

@router.get('/conf/list')
async def get_configs():
    return resp_success({'msg': 'trade'})


@router.get('/acct/list')
async def get_acct_list():
    pass

@router.get('/acct/detail')
async def get_acct_detail():
    pass

@router.post('/acct/start')
async def start_acct():
    pass

@router.post('/acct/stop')
async def stop_acct():
    pass

@router.post('/acct/operate')
async def operate_acct(data:dict):
    acctId = data['acctId']
    acct_mgr.send_to_client(acctId,data)
    pass