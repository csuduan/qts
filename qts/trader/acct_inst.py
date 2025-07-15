from typing import Any
from qts.common.object import *
from qts.common.constant import *
from qts.common.rpc.server import TcpServer
from qts.common.message import MsgType,Message,MsgHandler
from qts.common.event import event_engine,Event
from qts.common import get_logger,get_config
from .gateway import create_gateway, BaseGateway
from .utils import export_tick

log=get_logger(__name__)

class AcctInst():
    def __init__(self,config:AcctConf) -> None:
        self.gateway: BaseGateway = None
        self.acct_conf:AcctConf = config
        self.acct_info: AcctInfo = None
        self.acct_detail: AcctDetail = None
        self.rpc_server = None
        self.msg_handler:MsgHandler = self.create_rpc_handler()

    def start(self):
        self.acct_info = AcctInfo(conf=self.acct_conf)
        self.acct_detail = AcctDetail(acct_info=self.acct_info)
        #启动rcp server
        tcp_port = self.acct_conf.rpc_addr.split(":")[2]
        self.rpc_server = TcpServer(port=int(tcp_port),req_handler=self.__on_rpc_req,new_client_callback=self.__on_new_client)
        self.rpc_server.start()
        #注册事件
        event_engine.register_general(self.__on_event)
        #启动网关
        self.gateway = create_gateway('ctp', self.acct_detail)
        self.gateway.connect()

    def __on_new_client(self):
        #每次有新客户端连接，则主动一次就绪消息
        self.push_msg(MsgType.ON_CONNECTED,{})

    def __on_rpc_req(self,req:Message):
        handler = self.msg_handler.get_handler(req.type)
        if handler is not None:  # 如果找到了handler
            try:
                rsp = handler(req.data)
                rsp_msg = Message(type=req.type,code=0,data=rsp)
                return rsp_msg
            except Exception as e:
                log.error(f"Error handling request: {e}")
                return Message(type=req.type,code=-1,data=str(e))
        else:  # 如果没有找到对应的handler
            log.warning(f"No handler found for message type: {req.type}")
            return Message(type=req.type,code=-1,data="Handler not found")
   
    def __on_event(self,event:Event):
        if event.type == MsgType.ON_READY:
            #订阅已持仓的合约
            symbols = [p.symbol for p in self.acct_detail.position_map.values()]
            self.gateway.subscribe(SubscribeRequest(symbols=symbols))
        
        if event.type == MsgType.ON_TICK:
            if get_config("save_tick"):
                export_tick(event.data)
        #推送给admin
        self.push_msg(event.type,event.data)

    def push_msg(self,type,data):
        self.rpc_server.push(Message(type=type,data=data))

    def create_rpc_handler(self):         
        msg_handler = MsgHandler()
        @msg_handler.register(MsgType.CONNECT)
        def handle_connect(req) -> bool:
            self.gateway.connect()
            return True

        @msg_handler.register(MsgType.DISCONNECT)
        def handle_disconnect(req) -> bool:
            self.gateway.disconnect()
            return True
        
        @msg_handler.register(MsgType.GET_ACCT_INFO)
        def get_acct_info(req) -> AcctInfo:
            return self.acct_info
        
        @msg_handler.register(MsgType.GET_ACCT_DETAIL)
        def get_acct_detail(req) -> AcctDetail:
            return self.acct_detail

        @msg_handler.register(MsgType.GET_POSITIONS)
        def get_positions(req) -> list[Position]:
            positions = list(self.acct_detail.position_map.values())
            return positions


        @msg_handler.register(MsgType.GET_TRADES)
        def get_trades(req) -> list[TradeData]:
            trades = list(self.acct_detail.trade_map.values())
            return trades


        @msg_handler.register(MsgType.GET_ORDERS)
        def get_orders(req) -> list[OrderData]:
            orders = list(self.acct_detail.order_map.values())
            return orders

        @msg_handler.register(MsgType.GET_QUOTES)
        def get_quotes(req) -> list[TickData]:
            quotes = list(self.acct_detail.tick_map.values())
            return quotes

        @msg_handler.register(MsgType.SUBSCRIBE)
        def handle_subscribe(req) -> bool:
            symbol = req.get("symbol")
            sub_req = SubscribeRequest(symbol=symbol)
            self.gateway.subscribe(sub_req)
            return True

        @msg_handler.register(MsgType.SEND_ORDER)
        def insert_order(req:OrderRequest) -> bool:
            order = self.gateway.create_order(req)
            self.gateway.send_order(order)
            return True

        @msg_handler.register(MsgType.CANCEL_ORDER)
        def cancel_order(req:OrderCancel) -> bool:
            self.gateway.cancel_order(req)
            return True

        return msg_handler

    
    
