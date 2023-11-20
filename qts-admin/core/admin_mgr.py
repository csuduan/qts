from .acct_mgr import AcctMgr
from dao.my_dao import get_acct_configs, get_stragegy_configs
from utils import get_logger

logger = get_logger(__name__)


class AdminMgr(object):
    def __init__(self):
        logger.info("init admin_mgr")
