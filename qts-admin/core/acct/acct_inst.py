from core.rpc import RpcClient, RpcHandler
from core.rpc import MsgType


class AcctInst(object):
    def __init__(self, config):
        self.config = config
        self.acct_id = config['id']
        self.acct_client: RpcClient = None
        self.acct_info = None
        self.acct_detail = None

    def start_client(self):
        req_address = self.config['req_address']
        pub_address = self.config['pub_address']
        self.acct_client = RpcClient()
        self.acct_client.register_handler(self.create_handler())
        self.acct_client.start(req_address, pub_address)



    def stop_client(self):
        self.acct_client.stop()
        self.acct_client = None

    def connect(self):
        self.acct_client.req(MsgType.CONNECT)

    def disconnect(self):
        self.acct_client.req(MsgType.DISCONNECT)

    def close(self):
        self.acct_client.req(MsgType.CLOSE)

    def get_positions(self):
        self.acct_client.req(MsgType.GET_POSITIONS)
        pass

    def get_trades(self):
        self.acct_client.req(MsgType.GET_TRADES)
        pass

    def get_acct_info(self):
        self.acct_client.req(MsgType.GET_ACCT_INFO)
        pass

    def get_acct_detail(self):
        self.acct_client.req(MsgType.GET_ACCT_DETAIL)
        pass

    def send_order(self, order):
        self.acct_client.req(MsgType.SEND_ORDER, order)
        pass

    def cancel_order(self, orderid):
        self.acct_client.req(MsgType.CANCEL_ORDER, orderid)
        pass


    def create_handler(self):
        topic_handler = RpcHandler()

        @topic_handler.register_handler(MsgType.ON_TICK)
        def on_tick(data):
            config = self.config
            pass

        @topic_handler.register_handler(MsgType.ON_ORDER)
        def on_order(data):
            pass

        @topic_handler.register_handler(MsgType.ON_POSITION)
        def on_position(data):
            pass

        @topic_handler.register_handler(MsgType.ON_LOG)
        def on_log(data):
            pass

        return topic_handler
