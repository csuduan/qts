from typing import List,Dict
from core.event.event import EventEngine, Event
from gateway import create_gateway, BaseGateway
from qts.model.object import *
from qts.model.constant import *
from qts.tcp.server import TcpServer
from core.event import event_engine
from qts.model.message import MsgType,MsgHandler,Message




class AcctInst():
    def __init__(self) -> None:
        self.gateway: BaseGateway = None
        self.acct_info: AcctInfo = None
        self.acct_detail: AcctDetail = None
        self.tcp_server = None

    def start(self,config:AcctConf):
        """启动"""
        self.acct_conf = config
        self.acct_info = AcctInfo(id=config.id,group=config.group,name=config.name,enable=config.enable,conf=config)
        self.acct_detail = AcctDetail(acct_info=self.acct_info)


        #self.acct_event_handler = AcctEventHandler(self,event_engine)
        event_engine.register_general(self.on_event)

        self.gateway = create_gateway('ctp', self.acct_detail)
        self.gateway.connect()

    def add_tcp_server(self,tcp_server:TcpServer):
        self.tcp_server = tcp_server

    def push_msg(self,type,data):
        self.tcp_server.push(Message(type=type,data=data))
    
    def on_event(self,event:Event):
        if event.type == MsgType.ON_POSITIONS:
            #订阅已持仓的合约
            symbols = {(p.symbol, p.exchange) for p in self.acct_detail.position_map.values()}
            for symbol_pair in symbols:
                self.gateway.subscribe(SubscribeRequest(symbol=symbol_pair[0],exchange=symbol_pair[1]))
        #推送给admin
        self.push_msg(event.type,event.data)

    def order(self,req:OrderRequest):
        order :OrderData = OrderData(
            symbol=req.symbol,
            exchange=req.exchange,
            order_ref=self.gateway.next_order_ref(),
            direction=req.direction,
            offset=req.offset,
            price=req.price,
            volume=req.volume,
            type=req.type,
            status=Status.SUBMITTING
        )
        ret = self.gateway.send_order(order)
        if ret:
            self.acct_detail.order_map[order.order_ref] = order
        return ret

    

    
    
