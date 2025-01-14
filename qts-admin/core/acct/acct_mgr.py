from typing import Dict, List

from .acct_inst import AcctInst
from utils import get_logger
from config import get_setting
from model.acct import  AcctConf

log = get_logger(__name__)

'''
账户管理器
'''





class AcctMgr(object):
    def __init__(self):
        self.acct_configs = None
        self.acct_insts: Dict[str, AcctInst] = {}

    def start(self) :
        log.info("start acct mgr")
        self.acct_configs = get_setting("acct_list")
        for acct_conf in self.acct_configs:
            self.create_acct_inst(acct_conf)

    def create_acct_inst(self, config):
        acct_inst = AcctInst(config)
        self.acct_insts[config['id']] = acct_inst
        acct_inst.start_client()
        log.info(f'create acct_inst:{config['id']}')

    def get_acct_inst(self, acct_id):
        return self.acct_insts[acct_id]

    def get_acct_confs(self) -> List[AcctConf]:
        return  get_setting("acct_list")

    def get_acct_infos(self):
        list=[]
        for acctId in self.acct_insts.keys():
            list.append(self.acct_insts[acctId].acct_info)
        return list

