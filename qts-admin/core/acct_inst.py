from utils.rpc import TcpClient

class AcctInst(object):
    def __init__(self,config):
        self.config=config
        self.acct_id=config['acct_id']
        self.client=None
        self.acct_info=None
        self.acct_detail=None

    def start_client(self):
        host:str=self.config['host']
        port:int=self.config['port']
        self.client= TcpClient(host,port)
        pass

    def stop_client(self):
        self.client.close()
        self.client=None

    def request(self,req):
        pass

    def get_positions(self):
        pass

    def get_trades(self):
        pass

    def get_acct_info(self):
        pass

    def get_acct_detail(self):
        pass



