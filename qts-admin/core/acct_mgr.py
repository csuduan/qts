from utils import get_logger
from utils.rpc import TcpClient

logger = get_logger(__name__)

class AcctMgr(object):
    def __init__(self,acct_configs):
        self.acct_clients={}
        self.acct_configs = acct_configs

    def start_all(self):
        for acctId in self.acct_configs:
            self.start_client(acctId)
    def start_client(self,acctId):
        if acctId not in self.acct_clients:
            config = self.acct_configs[acctId]
            #todo
            # acct_Client = TcpClient(config['host'],config['port'])
            # acct_Client.connect()
            # self.acct_clients[acctId]=acct_Client

    def send_to_client(self,acctId,msg):
        if acctId in self.acct_clients and self.acct_clients[acctId].is_connected():
            acct_Client = self.acct_clients[acctId]
            acct_Client.send_msg(msg)
        else:
            logger.error("acctId not exist")



