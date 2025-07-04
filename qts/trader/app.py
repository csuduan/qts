import asyncio
import os,sys,signal,pickle,datetime
from typing import List

from qts.common.log import logger_utils
from qts.common.config import config
from qts.common.model.object import  ContractData,AcctConf
from qts.common.tcp.server import TcpServer

from .core.acct_inst import AcctInst
from .core.admin_engine import admin_engine


log = logger_utils.get_logger(__name__)




def handle_exit(signum, frame):
    log.info("接收到退出信号，正在清理...")
    # 写合约信息到缓存
    log.info("缓存合约信息...")
    contracts_map: dict[str, ContractData] = config.get_config('contracts')
    data = {'date': datetime.date.today(), 'contracts': contracts_map}
    data_path = config.get_config('data_path')
    with open(os.path.join(data_path, 'contracts.pkl'), 'wb') as f:
        pickle.dump(data, f)
    log.info("清理完成,即将退出系统")
    exit(0)

async def main(acctId: str = None):

    acct_confs: List = config.get_config('acct_confs')
    conf = next((conf for conf in acct_confs if conf['id'] == acctId), None)
    log.info(f"启动账户: {conf['id']}")

    acct_conf: AcctConf = AcctConf(**conf)
    acct_inst = AcctInst()
    acct_inst.start(acct_conf)
    #trade_engine.start(acct_conf)
    admin_engine.start(acct_conf.tcp_port,acct_inst)
    await asyncio.sleep(600)

# 注册信号处理器
signal.signal(signal.SIGINT, handle_exit)  # 捕捉 Ctrl+C
signal.signal(signal.SIGTERM, handle_exit)



