from typing import Dict, List
import json
from .acct_inst import AcctInst
    
from qts.common.config import config
from qts.common.log import get_logger
from qts.common.model.object import  AcctConf,AcctInfo
from qts.common.model.constant import EnumEncoder
from qts.common.dao import conf_dao
from fastapi import WebSocket
import asyncio


log = get_logger(__name__)

'''
账户管理器
'''
class AcctMgr(object):
    def __init__(self):
        self.acct_insts: Dict[str, AcctInst] = {}
        self.active_websockets: List[WebSocket] = []


    def start(self) :
        log.info("start acct mgr")
        acct_configs:List[AcctConf] = conf_dao.get_acct_configs()
        for acct_conf in acct_configs:
            self.create_inst(acct_conf)
            if acct_conf.enable:
                self.start_inst(acct_conf.id)
    
    def create_inst(self, config:AcctConf):
        acct_inst = AcctInst(config,self._ws_push)
        self.acct_insts[config.id] = acct_inst
        log.info(f'create acct_inst:{config.id}')

    def start_inst(self, acct_id):
        if acct_id not in self.acct_insts:
            return
        self.acct_insts[acct_id].start_inst()

        

    def get_acct_detail(self, acct_id,timestamp):
        if acct_id not in self.acct_insts:
            return
        return self.acct_insts[acct_id].get_acct_detail(timestamp)
    
    def get_ticks(self,acct_id,timestamp):
        if acct_id not in self.acct_insts:
            return
        return self.acct_insts[acct_id].get_quotes(timestamp)


    def get_acct_infos(self) -> List[AcctInfo]:
        list=[]
        for acctId in self.acct_insts.keys(): 
            inst = self.acct_insts[acctId]
            #查询状态
            inst.acct_info.status = inst.get_status()   
            list.append(inst.acct_info)
        return list
    def get_acct_inst(self, acct_id) -> AcctInst:
        return self.acct_insts[acct_id]
    
    def get_all_insts(self) -> List[AcctInst]:
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
                #ws.send_text(json.dumps(msg))
                loop.run_until_complete(ws.send_json(json_msg))

            except Exception as e:
                log.error(f"ws push error:{e}")
                self.active_websockets.remove(ws)

