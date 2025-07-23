from .acct_inst import AcctInst

from qts.common.rpc.server import Connection, TcpServer  
from qts.common import get_config
from qts.common import get_logger
from qts.common.object import  AcctConf,AcctInfo
from qts.common.message import MsgType
from qts.common.constant import EnumEncoder
from qts.common.dao import conf_dao
from qts.common.event import event_engine,Event
from fastapi import WebSocket
import asyncio


log = get_logger(__name__)

'''
账户管理器
'''
class AcctMgr():
    def __init__(self):
        self.acct_insts: dict[str, AcctInst] = {}
        self.active_websockets: list[WebSocket] = []
        self.rpc_server : TcpServer = None
        
    
    def on_connect(self,conn:Connection):
        acct_inst = self.acct_insts[conn.id]
        acct_inst.add_connection(conn)

    def start(self) :
        log.info("start acct mgr")
        acct_configs:list[AcctConf] = conf_dao.get_acct_configs()
        for acct_conf in acct_configs:
            self.create_inst(acct_conf)
        self.rpc_server = TcpServer("0.0.0.0",get_config('rpc_port'),self.on_connect)
        self.rpc_server.start()

    
    def create_inst(self, config:AcctConf):
        acct_inst = AcctInst(config,self._ws_push)
        self.acct_insts[config.id] = acct_inst
        log.info(f'create acct_inst:{config.id}')


    def get_acct_detail(self, acct_id,timestamp):
        if acct_id not in self.acct_insts:
            return
        return self.acct_insts[acct_id].get_acct_detail(timestamp) 
    
    def get_ticks(self,acct_id,timestamp):
        if acct_id not in self.acct_insts:
            return
        return self.acct_insts[acct_id].get_quotes(timestamp)


    def get_acct_infos(self) -> list[AcctInfo]:
        list=[]
        for acctId in self.acct_insts.keys(): 
            inst = self.acct_insts[acctId]
            #查询状态
            inst.acct_detail.acct_info.status = inst.inst_status 
            list.append(inst.acct_detail.acct_info)
        return list
    def get_acct_inst(self, acct_id) -> AcctInst:
        return self.acct_insts[acct_id]
    
    def get_all_insts(self) -> list[AcctInst]:
        return list(self.acct_insts.values())
    

    def _ws_push(self,json_msg):
        try:
            loop = asyncio.get_event_loop()
        except RuntimeError:
            # 如果没有事件循环，创建一个新的
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
        for ws in self.active_websockets:
            try:
                loop.run_until_complete(ws.send_json(json_msg))

            except Exception as e:
                log.error(f"ws push error:{e}")
                self.active_websockets.remove(ws)

