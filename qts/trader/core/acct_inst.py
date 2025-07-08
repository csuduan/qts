from typing import List,Dict
from qts.common.model.object import *
from qts.common.model.constant import *
from qts.common.tcp.server import TcpServer
from qts.common.model.message import MsgType,Message
from qts.common.event import event_engine,Event

from ..gateway import create_gateway, BaseGateway
from .rpc_handler import RpcHandler



class AcctInst():
    def __init__(self,config:AcctConf) -> None:
        self.gateway: BaseGateway = None
        self.acct_conf:AcctConf = config
        self.acct_info: AcctInfo = None
        self.acct_detail: AcctDetail = None
        self.rpc_server = None

    def start(self):
        """启动"""
        config = self.acct_conf
        self.acct_info = AcctInfo(id=config.id,group=config.group,name=config.name,enable=config.enable,conf=config)
        self.acct_detail = AcctDetail(acct_info=self.acct_info)
        #启动rcp server
        tcp_port = self.acct_conf.rpc_addr.split(":")[2]
        rpc_handler = RpcHandler(self)
        self.rpc_server = TcpServer(port=int(tcp_port),req_handler=rpc_handler.on_msg,new_client_callback=self.__on_new_client)
        self.rpc_server.start()
        #注册事件
        event_engine.register_general(self.__on_event)
        #启动网关
        self.gateway = create_gateway('ctp', self.acct_detail)
        self.gateway.connect()

    def __on_new_client(self):
        #每次有新客户端连接，则主动一次就绪消息
        self.push_msg(MsgType.ON_CONNECTED,{})
   
    def __on_event(self,event:Event):
        if event.type == MsgType.ON_POSITIONS:
            #订阅已持仓的合约
            symbols = {(p.symbol, p.exchange) for p in self.acct_detail.position_map.values()}
            for symbol_pair in symbols:
                self.gateway.subscribe(SubscribeRequest(symbol=symbol_pair[0],exchange=symbol_pair[1]))
        #推送给admin
        self.push_msg(event.type,event.data)

    def push_msg(self,type,data):
        self.rpc_server.push(Message(type=type,data=data))



    
    
