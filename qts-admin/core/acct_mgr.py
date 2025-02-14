from typing import Dict, List
import json
from .acct_inst import AcctInst
from qts.log import get_logger    
from config import get_setting
from qts.model.object import  AcctConf,AcctInfo
from fastapi import WebSocket

log = get_logger(__name__)

'''
账户管理器
'''



class AcctMgr(object):
    def __init__(self):
        self.acct_configs = None
        self.acct_insts: Dict[str, AcctInst] = {}
        self.active_websockets: List[WebSocket] = []


    def start(self) :
        log.info("start acct mgr")
        self.acct_configs = get_setting("acct_list")
        for acct_conf in self.acct_configs:
            conf: AcctConf = AcctConf(**acct_conf)
            self.create_acct_inst(conf)
            if conf.enable:
                self.start_inst(conf.id)
        


    def create_acct_inst(self, config):
        acct_inst = AcctInst(config,self._ws_push)
        self.acct_insts[config.id] = acct_inst
        #acct_inst.start_client()
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

    def get_acct_confs(self) -> List[AcctConf]:
        return  get_setting("acct_list")

    def get_acct_infos(self) -> List[AcctInfo]:
        list=[]
        for acctId in self.acct_insts.keys():      
            list.append(self.acct_insts[acctId].acct_info)
        return list
    def get_acct_inst(self, acct_id) -> AcctInst:
        return self.acct_insts[acct_id]
    

    def _ws_push(self,msg):
        for ws in self.active_websockets:
            try:
                #ws.send_text(json.dumps(msg))
                ws.send_json(msg)
            except:
                self.active_websockets.remove(ws)

