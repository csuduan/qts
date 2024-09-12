from .acct_mgr import AcctMgr
from dao.my_dao import get_acct_configs, get_stragegy_configs
from utils import get_logger

logger = get_logger(__name__)

logger.info("start admin_mgr")

#acct_configs = get_acct_configs()
acct_configs={}
acct_mgr = AcctMgr(acct_configs)

