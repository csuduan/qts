from typing import Any
from qts.common.object import *
from qts.common.constant import *
from qts.common.rpc.client import TcpClient
from qts.common.message import MsgType
from qts.common.event import event_engine,Event
from qts.common import get_logger,get_config
from .gateway import create_gateway, BaseGateway
from .utils import export_tick

log=get_logger(__name__)

class TraderInst():
    def __init__(self,config:AcctConf) -> None:
        self.gateway: BaseGateway = None
        self.acct_conf:AcctConf = config
        self.acct_detail: AcctDetail = None
        self.rpc_client = None
        #self.msg_handler:MsgHandler = self.create_rpc_handler()

    def start(self):
        acct_info = AcctInfo(conf=self.acct_conf)
        self.acct_detail = AcctDetail(acct_info=acct_info)
        #启动rcp server
        host,port = get_config("rpc_addr").split(":")
        self.rpc_client = TcpClient(host=host,port=int(port),id = self.acct_conf.id)
        self.rpc_client.start()
        #注册事件
        event_engine.register_general(self.__on_rpc_event)
        event_engine.register_general(self.__on_gen_event)
        #启动网关
        api_type = self.acct_conf.td_addr.split("|")[0]
        self.gateway = create_gateway(api_type, self.acct_detail)
        self.gateway.connect()

    def __on_rpc_event(self,event:Event):
        """处理rpc事件"""
        match event.type:
            case MsgType.REQ_DISCONNECT:
                self.gateway.disconnect()
            case MsgType.REQ_CONNECT:
                self.gateway.connect()
            case MsgType.REQ_SEND_ORDER:
                order_req:OrderData = self.gateway.create_order(event.data)          
                self.gateway.send_order(order_req)
            case MsgType.REQ_CANCEL_ORDER:
                req_cancel:OrderCancel = event.data
                self.gateway.cancel_order(req_cancel)
                pass
            case MsgType.REQ_SUBSCRIBE:
                req_sub:SubscribeRequest = event.data
                self.gateway.subscribe(req_sub)
            case _:
                pass 

    def __on_gen_event(self,event:Event):
        """处理通用事件"""
        match event.type:
            case MsgType.ON_LOG:
                self.push_msg(event.type,event.data)
            case MsgType.ON_CONNECTED:
                #连接服务端成功后推送一次账户信息
                self.push_msg(MsgType.ON_ACCT_DETAIL,self.acct_detail)       
            case MsgType.ON_STATUS:
                self.push_msg(MsgType.ON_ACCT_INFO,self.acct_detail.acct_info)
            case MsgType.ON_READY:
                symbols = [p.symbol for p in self.acct_detail.position_map.values()]
                self.gateway.subscribe(SubscribeRequest(symbols=symbols))
                self.push_msg(MsgType.ON_ACCT_DETAIL,self.acct_detail)
            case MsgType.ON_POSITION:
                self.push_msg(event.type,event.data)
            case MsgType.ON_TICK:
                if get_config("save_tick"):
                    export_tick(event.data)
                self.push_msg(event.type,event.data)
            case MsgType.ON_ORDER:
                self.push_msg(event.type,event.data)
            case MsgType.ON_TRADE:
                self.push_msg(event.type,event.data)
            case _:
                pass
            

    def push_msg(self,type,data):
        self.rpc_client.send(type,data)

    
    
