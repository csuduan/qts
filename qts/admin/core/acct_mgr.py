from fastapi import WebSocket
import asyncio

from qts.common.rpc.server import Connection, TcpServer  
from qts.common import get_config
from qts.common import get_logger
from qts.common.object import  AcctConf,AcctInfo
from qts.common.message import MsgType
from qts.common.constant import EnumEncoder
from qts.common.dao import conf_dao
from qts.common.event import event_engine,Event

from .acct_inst import AcctInst
from .ws_mgr import WsMgr


log = get_logger(__name__)

'''
账户管理器
'''
class AcctMgr():
    def __init__(self):
        self.acct_insts: dict[str, AcctInst] = {}
        self.rpc_server : TcpServer = None
        self.ws_mgr = WsMgr()
        
    
    def on_connect(self,conn:Connection):
        if conn.id not in self.acct_insts:
            log.error(f"找不到账户{conn.id},请检查账户是否已禁用")
            return
        acct_inst = self.acct_insts[conn.id]
        acct_inst.add_connection(conn)

    def start(self) :
        log.info("start acct mgr")
        acct_configs:list[AcctConf] = conf_dao.get_acct_configs()
        for acct_conf in acct_configs:
            if acct_conf.enable:
                self.create_inst(acct_conf)
        self.rpc_server = TcpServer("0.0.0.0",get_config('rpc_port'),self.on_connect)
        self.rpc_server.start()
        self.ws_mgr.start()

    
    def create_inst(self, config:AcctConf):
        acct_inst = AcctInst(config,self.ws_mgr)
        self.acct_insts[config.id] = acct_inst
        log.info(f'create acct_inst:{config.id}')


    def get_acct_detail(self, acct_id,timestamp):
        if acct_id not in self.acct_insts:
            return
        return self.acct_insts[acct_id].get_acct_detail(timestamp) 
    

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
        self.ws_mgr.push_msg(json_msg)

