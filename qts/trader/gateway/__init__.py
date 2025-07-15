from qts.common.object import AcctDetail
from .base_gateway import BaseGateway
from .ctp.ctp_gateway import  CtpGateway


def create_gateway(type:str, acct_detail: AcctDetail ) -> BaseGateway:
    """创建接口"""
    if type == "ctp":
        return CtpGateway(acct_detail)
    else:
        return None