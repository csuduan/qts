from .acct_inst import AcctInst
from utils import get_logger

logger = get_logger(__name__)

'''
账户管理器
'''
class AcctMgr(object):
    def __init__(self,acct_configs):
        logger.info("init acct_mgr...")
        self.acct_configs = acct_configs
        self.acct_insts ={}
        for acct_id in self.acct_configs:
            self.create_acct_inst(self.acct_configs[acct_id])

    def create_acct_inst(self,config):
        acct_inst = AcctInst(config)
        self.acct_insts[config['acct_id']] = acct_inst
        logger.error(f'create acct_inst:{config['acct_id']}')

    def get_acct_inst(self,acct_id):
        return self.acct_insts[acct_id]



