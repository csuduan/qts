from fastapi import status
from fastapi.responses import JSONResponse
from typing import Union
from .biz_exception import  BizException


def resp_success(data: Union[list, dict, str]):
    '''
    成功响应
    :param data:
    :return:
    '''
    return JSONResponse(
        status_code=status.HTTP_200_OK,
        content={
            'code': 0,
            'message': 'success',
            'data': data
        }
    )


def resp_error(err_code: int, err_msg: str):
    '''
    错误响应
    :param err_code:
    :param err_msg:
    :return:
    '''
    return JSONResponse(
        status_code=status.HTTP_200_OK,
        content={
            'code': err_code,
            'message': err_msg
        }
    )

async  def http_exception_handler(request,ex:Exception):
    return resp_error(9,str(ex))

async  def biz_exception_handler(request,ex:BizException):
    return resp_error(ex.err_code,ex.err_msg)
