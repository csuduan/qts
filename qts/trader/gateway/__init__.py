from qts.common.object import AcctDetail
from qts.common import set_config
from .base_gateway import BaseGateway

def create_gateway(type:str, acct_detail: AcctDetail ) -> BaseGateway:
    """创建接口"""
    if type == "ctp" or type == "rohon":
        #ctp 及兼容接口
        set_config('gateway.type',type)
        from .ctp.ctp_gateway import  CtpGateway
        return CtpGateway(acct_detail)
    else:
        raise Exception(f"不支持的接口类型:{type}")