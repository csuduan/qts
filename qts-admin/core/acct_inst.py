
from qts.model.object import  AcctConf,AcctInfo,Position,TickData
import subprocess
import os
import psutil
import datetime
from typing import Any, List,Dict
from qts.tcp.client import TcpClient
from qts.model.message import MsgHandler,MsgType,Message
from qts.model.object import TradeData,TickData
from model.object import AcctDetail

from qts.log import get_logger

log = get_logger(__name__)

class AcctInst(object):
    def __init__(self, config,ws_callback):
        self.config: AcctConf = config
        self.acct_id = config.id
        self.acct_client: TcpClient = None
        acct_info = AcctInfo(id=config.id,group=config.group,name=config.name,enable=config.enable,conf=config)
        self.acct_info: AcctInfo = acct_info
        self.acct_detail: AcctDetail = AcctDetail(acct_info=acct_info)
        self.tick_map: Dict[str,TickData] ={}
        self.tick_timestamp=""
        self.ws_callback = None


    def start_inst(self):   
        self.push_handler = self.create_handler()  
        if self.config.enable:          
            self.acct_client = TcpClient("127.0.0.1",self.config.tcp_port,self._push_handler)
            self.acct_client.start()

        


    def stop_inst(self):
        self.acct_client.stop()
        self.acct_client = None

    def connect(self):
        self.acct_client.request(Message(MsgType.CONNECT))

    def disconnect(self):
        self.acct_client.request(Message(MsgType.DISCONNECT))

    def close(self):
        self.acct_client.request(Message(MsgType.CLOSE))
    
    def _push_handler(self,data:Message):
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
        self.acct_client.request(Message(MsgType.GET_TRADES))
        pass

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
        self.acct_detail.acct_info = self.get_acct_info()
        self.acct_detail.positions = self.get_positions()
        self.acct_detail.trades = self.get_trades()
        self.acct_detail.orders = self.get_orders()
        self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log.info(f"同步账户信息{self.acct_id}成功")
        

    def send_order(self, order):
        self.acct_client.request(Message(MsgType.SEND_ORDER, order))
        pass

    def cancel_order(self, orderid):
        self.acct_client.request(Message(MsgType.CANCEL_ORDER, orderid))
        pass

    def send_ws_msg(self,type:str,msg:Any):
        if self.ws_callback:
            self.ws_callback({"type":type,"data":msg})


    def create_handler(self):
        topic_handler = MsgHandler()

        @topic_handler.register(MsgType.ON_READY)
        def on_acct(data):
            #self.acct_detail.acct_info = data
            #self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            #log.info(f"同步账户信息{self.acct_id}成功")
            self._fetch_acct_info();
            self.send_ws_msg("on_acct",self.acct_detail)

        @topic_handler.register(MsgType.ON_TICK)
        def on_tick(data:TickData):
            self.tick_map[data.symbol] = data
            data.localtime = datetime.datetime.now().strftime("%Y%m%d %H:%M:%S")
            self.send_ws_msg("on_tick",data)


        @topic_handler.register(MsgType.ON_ORDER)
        def on_order(data):
            pass

        @topic_handler.register(MsgType.ON_POSITION)
        def on_position(data):
            self.acct_detail.positions = data
            self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            pass


        @topic_handler.register(MsgType.ON_TRADE)
        def on_trade(data:TradeData):
            # Update or add trade data
            found = False
            for i, trade in enumerate(self.acct_detail.trades):
                if trade.id == data.id:
                    self.acct_detail.trades[i] = data
                    found = True
                    break
            
            if not found:
                self.acct_detail.trades.append(data)
            self.acct_detail.timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            pass

        return topic_handler
