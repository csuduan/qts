
import os
from qts.admin.core.ws_mgr import WsMgr
from qts.common.object import  AcctConf,AcctInfo,Position,TickData
import json
import queue
import datetime
from apscheduler.schedulers.background import BackgroundScheduler
import time
from typing import Any, Callable, List,Dict
from qts.common.rpc.server import Connection
from qts.common.message import MsgHandler,MsgType,Message
from qts.common.event import event_engine,Event
from qts.common.object import TradeData,TickData,AcctDetail,OrderData,OrderRequest,Exchange,SubscribeRequest
from qts.common import get_config
from qts.common import get_logger

log = get_logger(__name__)

LOG_BUFF_SIZE = 1000
'''
账户实例
'''
class AcctInst(object):
    def __init__(self, config:AcctConf,ws_msg:WsMgr):
        self.config: AcctConf = config
        self.acct_id = config.id
        acct_info = AcctInfo(id=config.id,group=config.group,name=config.name,enable=config.enable,status=False,conf=config)
        #self.acct_info: AcctInfo = acct_info
        self.ws_mgr = ws_msg
        self.acct_detail: AcctDetail = AcctDetail(acct_info=acct_info)
        self.acct_connection: Connection = None
        self.log_buffer:list[str] = []
        self.alarms:list[str] = []
        self.msg_handler = self.create_handler()
        self.scheduler = BackgroundScheduler()
        self.scheduler.add_job(self.push_task, 'interval', seconds=3)  # 每3秒触发
        self.scheduler.start()
        self._last_push_time = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]

        
    @property
    def inst_status(self):
        return self.acct_connection and self.acct_connection.is_active()

    def add_connection(self,conn:Connection):
        self.acct_connection = conn
        self.acct_connection.on_message = self.on_message
        self.acct_detail.acct_info.status = True

    def on_message(self,msg):
        type = msg['type']
        data = msg['data']
        if type != MsgType.ON_TICK and type != MsgType.ON_LOG:
            log.info(f"{self.acct_id}收到同步消息:{type}")

        if type==MsgType.ON_LOG:
            self.log_buffer.append(data)
            if len(self.log_buffer)>LOG_BUFF_SIZE:
                self.log_buffer.pop(0)
            if 'ERROR' in data:
                self.alarms.append(data)

        handler = self.msg_handler.get_handler(type)
        if handler:
            handler(data) 

    def connect(self):
        """连接接口"""
        self.acct_connection.send(MsgType.REQ_CONNECT,{})

    def disconnect(self):
        """断开接口"""
        self.acct_connection.send(MsgType.REQ_DISCONNECT,{})

    def subscribe(self,subs:SubscribeRequest):
        """订阅行情"""
        self.acct_connection.send(MsgType.REQ_SUBSCRIBE,subs)

    def send_order(self, order:OrderRequest):
        """发送订单"""
        self.acct_connection.send(MsgType.REQ_SEND_ORDER, order)

    def cancel_order(self, orderid):
        """取消订单"""
        self.acct_connection.send(MsgType.REQ_CANCEL_ORDER, orderid)
 
    def get_acct_detail(self,timestamp:str):
        if timestamp is None or timestamp<self.acct_detail.acct_info.timestamp:
            return self.acct_detail
        else:
            return {}

    def get_acct_detail(self):
        return self.acct_detail


    def send_ws_msg(self,type:str,json_msg:Any):
        #event_engine.put(MsgType.ON_WS,{"type":type,"acct_id":self.acct_id,"data":json_msg})
        #self.push_callback({"type":type,"acct_id":self.acct_id,"data":json_msg})
        self.ws_mgr.push_msg({"type":type,"acct_id":self.acct_id,"data":json_msg})

    def push_task(self):
        if not self.inst_status:
            return
        if self._last_push_time  < self.acct_detail.acct_info.timestamp:
            self._last_push_time = self.acct_detail.acct_info.timestamp
            self.send_ws_msg("on_acct",json.loads(self.acct_detail.acct_info.json()))



    def create_handler(self):
        topic_handler = MsgHandler()
        @topic_handler.register(MsgType.ON_CONNECTED)
        def on_connect(data):
            self.send_ws_msg("on_acct",json.loads(self.acct_detail.acct_info.json()))

        @topic_handler.register(MsgType.ON_READY)
        def on_ready(data:AcctDetail):
            self.acct_detail = data
            self.acct_detail.acct_info = data.acct_info
            self.acct_detail.update()
            self.send_ws_msg("on_acct_detail",json.loads(self.acct_detail.json()))

        @topic_handler.register(MsgType.ON_ACCT_INFO)
        def on_acct_info(data:AcctInfo):
            self.acct_detail.acct_info = data
            self.acct_detail.update()
            self.send_ws_msg("on_acct",json.loads(self.acct_detail.acct_info.json()))

        @topic_handler.register(MsgType.ON_ACCT_DETAIL)
        def on_acct_detail(data:AcctDetail):
            self.acct_detail = data
            self.acct_detail.update()
            self.send_ws_msg("on_acct_detail",json.loads(self.acct_detail.json()))

        @topic_handler.register(MsgType.ON_TICK)
        def on_tick(data:TickData):
            self.acct_detail.tick_map[data.symbol] = data
            #data.localtime = datetime.datetime.now().strftime("%Y%m%d %H:%M:%S")
            self.acct_detail.update()
            self.send_ws_msg("on_tick",json.loads(data.json()))
        
        @topic_handler.register(MsgType.ON_LOG)
        def on_log(data:str):
            self.send_ws_msg("on_log",data)

        @topic_handler.register(MsgType.ON_ORDER)
        def on_order(data:OrderData):
            self.acct_detail.order_map[data.order_ref] = data
            if data.deleted:
                self.acct_detail.order_map.pop(data.order_ref,None)
            self.acct_detail.update()
            self.send_ws_msg("on_order",json.loads(data.json()))

        @topic_handler.register(MsgType.ON_POSITION)
        def on_position(data:Position):
            self.acct_detail.position_map[data.id] = data
            self.acct_detail.update()
            self.send_ws_msg("on_position",json.loads(data.json()))
            log.info(f"on_position:{data}")

        @topic_handler.register(MsgType.ON_TRADE)
        def on_trade(data:TradeData):
            # Update or add trade data
            self.acct_detail.trade_map[data.id] = data
            self.acct_detail.update()
            self.send_ws_msg("on_trade",json.loads(data.json()))
            log.info(f"on_trade:{data}")
        return topic_handler
