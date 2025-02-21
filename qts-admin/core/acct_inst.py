
from qts.model.object import  AcctConf,AcctInfo,Position,TickData
import subprocess
import os
import psutil
import json
import datetime
from typing import Any, List,Dict
from qts.tcp.client import TcpClient
from qts.model.message import MsgHandler,MsgType,Message
from qts.model.object import TradeData,TickData,AcctDetail,OrderData,OrderRequest,Exchange

from qts.log import get_logger

log = get_logger(__name__)

class AcctInst(object):
    def __init__(self, config,ws_callback):
        self.config: AcctConf = config
        self.acct_id = config.id
        self.acct_client: TcpClient = None
        acct_info = AcctInfo(id=config.id,group=config.group,name=config.name,enable=config.enable,status=False,conf=config)
        self.acct_info: AcctInfo = acct_info
        self.acct_detail: AcctDetail = AcctDetail(acct_info=acct_info)
        self.tick_map: Dict[str,TickData] ={}
        self.tick_timestamp=""
        self.inst_status = False
        self.ws_callback = ws_callback


    def start_inst(self):   
        self.push_handler = self.create_handler()  
        if self.config.enable:          
            self.acct_client = TcpClient("127.0.0.1",self.config.tcp_port,self._push_handler)
            self.acct_client.start()

        


    def stop_inst(self):
        self.acct_client.stop()
        self.acct_client = None

    def get_status(self):
        return self.acct_client and self.acct_client.is_connected()

    def connect(self):
        self.acct_client.request(Message(MsgType.CONNECT))

    def disconnect(self):
        self.acct_client.request(Message(MsgType.DISCONNECT))

    def subscribe(self,symbol:str):
        self.acct_client.request(Message(MsgType.SUBSCRIBE,data={'symbol':symbol}))

    def close(self):
        self.acct_client.request(Message(MsgType.CLOSE))
    
    def _push_handler(self,data:Message):
        if data.type != MsgType.ON_TICK:
            log.info(f"{self.acct_id}收到同步消息:{data.type}")
        handler = self.push_handler.get_handler(data.type)
        if handler:
            handler(data.data)
        

    def get_positions(self)->List[Position]:
        rsp:Message = self.acct_client.request(Message(MsgType.GET_POSITIONS))
        if rsp.code != 0:
            raise Exception(rsp.data)
        return rsp.data

    def get_orders(self):
        self.acct_client.request(Message(MsgType.GET_ORDERS))
        pass

    def get_trades(self):
        rsp:Message = self.acct_client.request(Message(MsgType.GET_TRADES))
        if rsp.code != 0:
            raise Exception(rsp.data)
        return rsp.data

    def get_acct_info(self)->AcctInfo:
        rsp:Message = self.acct_client.request(Message(MsgType.GET_ACCT_INFO))
        if rsp.code != 0:
            raise Exception(rsp.data)
        return rsp.data

    
    def get_quotes(self,timestamp:str)->List[TickData]:
        result = []
        for tick in self.tick_map.values():
            if tick.localtime and tick.localtime > timestamp:
                result.append(tick)
        return result

    def get_acct_detail(self,timestamp:str):
        if timestamp is None or timestamp<self.acct_detail.timestamp:
            return self.acct_detail
        else:
            return {}
    
    def _fetch_acct_info(self):
        rsp:Message = self.acct_client.request(Message(MsgType.GET_ACCT_DETAIL))
        if rsp.code != 0:
            raise Exception(rsp.data)
        self.acct_detail = rsp.data
        self.acct_info = self.acct_detail.acct_info
        log.info(f"同步账户信息{self.acct_id}成功")
        

    def send_order(self, order:OrderRequest):
        self.acct_client.request(Message(MsgType.SEND_ORDER, order))
        pass

    def cancel_order(self, orderid):
        self.acct_client.request(Message(MsgType.CANCEL_ORDER, orderid))
        pass

    def send_ws_msg(self,type:str,json_msg:Any):
        if self.ws_callback:
            self.ws_callback({"type":type,"acct_id":self.acct_id,"data":json_msg})


    def create_handler(self):
        topic_handler = MsgHandler()

        @topic_handler.register(MsgType.ON_CONNECTED)
        def on_connect(data):
            self._fetch_acct_info();
            self.send_ws_msg("on_acct",json.loads(self.acct_info.json()))

        @topic_handler.register(MsgType.ON_READY)
        def on_ready(data:AcctDetail):
            self.acct_detail = data
            self.acct_detail.acct_info = data.acct_info
            self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            self.send_ws_msg("on_acct_detail",json.loads(self.acct_detail.json()))

        @topic_handler.register(MsgType.ON_ACCT_INFO)
        def on_acct_info(data:AcctInfo):
            self.acct_info=data
            self.acct_detail.acct_info = data
            self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            self.send_ws_msg("on_acct",json.loads(data.json()))

        @topic_handler.register(MsgType.ON_TICK)
        def on_tick(data:TickData):
            self.tick_map[data.symbol] = data
            data.localtime = datetime.datetime.now().strftime("%Y%m%d %H:%M:%S")
            self.send_ws_msg("on_tick",json.loads(data.json()))


        @topic_handler.register(MsgType.ON_ORDER)
        def on_order(data:OrderData):
            self.acct_detail.order_map[data.order_ref] = data
            if data.deleted:
                self.acct_detail.order_map.pop(data.order_ref,None)
            
            self.send_ws_msg("on_order",json.loads(data.json()))

        @topic_handler.register(MsgType.ON_POSITIONS)
        def on_positions(data:dict[str,Position]):
            self.acct_detail.position_map=data
            self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            #self.send_ws_msg("on_positions",json.loads(self.acct_detail.json()))

        @topic_handler.register(MsgType.ON_POSITION)
        def on_position(data:Position):
            self.acct_detail.position_map[data.id] = data
            self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            self.send_ws_msg("on_position",json.loads(data.json()))
            log.info(f"on_position:{data}")

        @topic_handler.register(MsgType.ON_TRADES)
        def on_trades(data:dict[str,TradeData]):
            # Update or add trade data
            self.acct_detail.trade_map = data
            self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            #self.send_ws_msg("on_trades",json.loads(data.json()))
            log.info(f"on_trades:{data}")

        @topic_handler.register(MsgType.ON_TRADE)
        def on_trade(data:TradeData):
            # Update or add trade data
            self.acct_detail.trade_map[data.id] = data
            self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            self.send_ws_msg("on_trade",json.loads(data.json()))
            log.info(f"on_trade:{data}")

        return topic_handler
