from .app import acct_mgr
from qts.common import get_logger

log = get_logger(__name__)

def connect_api_job():
    log.info('触发任务：connect_api_job')
    for inst in acct_mgr.get_all_insts():
        if inst.inst_status :
            inst.connect()

def disconnect_api_job():
    log.info('触发任务：disconnect_api_job')
    for inst in acct_mgr.get_all_insts():
        if inst.inst_status :
            inst.disconnect()
