from fastapi import APIRouter,Request
from pydantic import BaseModel,Extra
from utils import log_utils
from common.resp import  resp_success
from config import  get_setting

logger = log_utils.get_logger(__name__)
router = APIRouter(prefix='/v1/sys')


@router.post('/user/login')
async def lonin():
    data={
        'avatar':'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif',
        'username':'Admin',
        'nickname':'Admin',
        'roles':['admin'],
        'permisions':[],
        'accessToken': 'admin-token',
        'refreshToken': 'admin-token'
    }
    return resp_success(data)


@router.post('/user/logout')
async def lonout():
    return resp_success('success')

@router.get('/user/info')
async def get_userInfo():
    useInfo = {
        'avatar':'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif',
        'name':'Admin',
        'roles':['admin'],
        'permisions':[]
    }
    return resp_success(useInfo)

@router.get('/router/dynamic')
async def get_router():
    '''
    获取动态路由表
    :return:
    '''
    routers = get_setting('routers')
    return resp_success(routers)

@router.get('/jobs')
async def get_jobs():
    '''
    获取任务列表
    :return:
    '''
    jobs=[]
    return resp_success(jobs)


