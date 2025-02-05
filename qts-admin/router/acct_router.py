from fastapi import APIRouter, Request
from pydantic import BaseModel, Extra
from utils import log_utils
from common.resp import resp_success

from core.acct import acct_mgr
from model.acct import Position
from model.constant import Exchange, Direction

router = APIRouter(prefix='/v1/acct')


@router.get('/conf/list')
async def get_configs():
    '''
    返回配置账户列表
    :return:
    '''
    list = acct_mgr.get_acct_confs()
    return resp_success(list)


@router.put('/conf')
async def get_configs(config):
    '''
    添加账户配置
    :return:
    '''
    return resp_success()


@router.delete('/conf')
async def get_configs(acct_id: str):
    '''
    删除账户配置
    :return:
    '''
    return resp_success()


@router.get('/inst/list')
async def get_acct_list():
    '''
    返回账户实例列表
    :return:
    '''
    list = acct_mgr.get_acct_infos()
    return resp_success(list)

@router.get('/inst/info')
async def get_acct_detail(acct_id: str):
    '''
    返回账户实例信息
    :param acct_id: 账户编号
    :return:
    '''
    info = acct_mgr.get_acct_info(acct_id).acct_info
    return resp_success(info)


@router.get('/inst/detail')
async def get_acct_detail(acct_id: str):
    '''
    返回账户实例详情
    :param acct_id: 账户编号
    :return:
    '''
    detail = {}

    return resp_success(detail)



@router.get('/inst/positions')
async def get_acct_positions(acct_id: str):
    '''
    返回账户持仓
    :param acct_id: 账户编号
    :return:
    '''
    positions = {}

    return resp_success(positions)

@router.get('/inst/orders')
async def get_acct_orders(acct_id: str):
    '''
    返回账户报单
    :param acct_id: 账户编号
    :return:
    '''
    orders = {}
    return resp_success(orders)

@router.get('/inst/ticks')
async def get_acct_ticks(acct_id: str):
    '''
    返回账户行情
    :param acct_id: 账户编号
    :return:
    '''
    ticks = {}
    return resp_success(ticks)


@router.post('/inst/start')
async def start_acct():
    pass


@router.post('/inst/stop')
async def stop_acct():
    pass


@router.post('/inst/connect')
async def connect_acct(acct_id: str):
    acct_mgr.get_acct_inst(acct_id).connect()
    pass


@router.post('/inst/disconnect')
async def disconnect_acct(acct_id: str):
    acct_mgr.get_acct_inst(acct_id).disconnect()
    pass
