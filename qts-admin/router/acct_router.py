import datetime
from fastapi import APIRouter, Request
from pydantic import BaseModel, Extra
from common.resp import resp_success

from core import acct_mgr
from qts.model.object import *
from qts.model.constant import Exchange, Direction

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
async def get_inst_list():
    '''
    返回账户实例列表
    :return:
    '''
    list = acct_mgr.get_acct_infos()
    return resp_success(list)


@router.get('/inst/detail')
async def get_acct_detail(acct_id: str,timestamp: str):
    '''
    返回账户实例详情
    :param acct_id: 账户编号
    :return:
    '''
    detail = acct_mgr.get_acct_detail(acct_id,timestamp)

    return resp_success(detail)



@router.get('/inst/positions')
async def get_acct_positions(acct_id: str):
    '''
    返回账户持仓
    :param acct_id: 账户编号
    :return:
    '''
    positions = acct_mgr.get_acct_inst(acct_id).get_positions()
    return resp_success(positions)

@router.get('/inst/orders')
async def get_acct_orders(acct_id: str):
    '''
    返回账户报单
    :param acct_id: 账户编号
    :return:
    '''
    orders = acct_mgr.get_acct_inst(acct_id).get_orders()
    return resp_success(orders)

@router.get('/inst/quotes')
async def get_acct_ticks(acct_id: str,timestamp: str):
    '''
    返回账户行情
    :param acct_id: 账户编号
    :return:
    '''
    ticks = acct_mgr.get_acct_inst(acct_id).get_quotes(timestamp)
    resp = resp_success(ticks)
    return resp

@router.get('/inst/trades')
async def get_acct_trades(acct_id: str):
    '''
    返回账户成交
    :param acct_id: 账户编号
    :return:
    '''
    trades = acct_mgr.get_acct_inst(acct_id).get_trades()
    return resp_success(trades)

@router.get('/inst/operate')
async def disconnect_acct(acct_id: str,op_type: AcctOpType):
    acct_inst = acct_mgr.get_acct_inst(acct_id)
    if not acct_inst:
        raise Exception(f"账户实例不存在: {acct_id}")

    if op_type == AcctOpType.CONNECT:
        acct_inst.connect()
    elif op_type == AcctOpType.DISCONNECT:
        acct_inst.disconnect()
    return resp_success()

@router.post('/inst/subscribe')
async def sub_contract(data:dict):
    acct_id = data.get('acct_id')
    symbol = data.get('symbol')
    acct_inst = acct_mgr.get_acct_inst(acct_id)
    if not acct_inst:
        raise Exception(f"账户实例不存在: {acct_id}")
    acct_inst.subscribe(symbol)
    return resp_success()

@router.post('/inst/order')
async def send_order(acct_id:str,data:OrderRequest):
    acct_inst = acct_mgr.get_acct_inst(acct_id)
    if not acct_inst:
        raise Exception(f"账户实例不存在: {acct_id}")
    acct_inst.send_order(data)
    return resp_success()