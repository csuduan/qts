from core.rpc import RpcClient, RpcHandler
from core.rpc import MsgType
from model.acct import  AcctConf,AcctInfo
import subprocess
import os
import psutil




class AcctInst(object):
    def __init__(self, config):
        self.config: AcctConf = config
        self.acct_id = config.id
        self.acct_client: RpcClient = None
        acct_info = AcctInfo(id=config.id,group=config.group,name=config.name,enable=config.enable,conf=config)
        self.acct_info: AcctInfo = acct_info
        self.acct_detail = None


    def start_inst(self):     
        #连接账户进程
        req_address = self.config.req_address
        pub_address = self.config.pub_address
        self.acct_client = RpcClient()
        self.acct_client.register_handler(self.create_handler())
        self.acct_client.start(req_address, pub_address)

    


    def stop_inst(self):
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
