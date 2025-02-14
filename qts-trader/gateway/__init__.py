from core.event.event import EventEngine
from model.object import AcctConf,AcctInfo
from .base_gateway import BaseGateway
from .ctp.ctp_gateway import  CtpGateway


def create_gateway(type:str,event_engine: EventEngine, acct_info :AcctInfo) -> BaseGateway:
    """创建接口"""
    if type == "ctp":
        return CtpGateway(event_engine,acct_info)
    else:
        return None