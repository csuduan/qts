import asyncio
import os,signal,pickle,datetime
from qts.common import get_logger,get_config
from qts.common.object import  ContractData
from qts.common.dao import conf_dao
from qts.common.message import MsgType
from qts.common.event import event_engine,Event
from qts.common.logger import add_custom_sink,INFO
from .trader_inst import TraderInst

log = get_logger(__name__)

def handle_exit(signum, frame):
    log.info("接收到退出信号，正在清理...")
    # 写合约信息到缓存
    log.info("缓存合约信息...")
    contracts_map: dict[str, ContractData] = get_config('contracts')
    data = {'date': datetime.date.today(), 'contracts': contracts_map}
    data_path = get_config('data_path')
    with open(os.path.join(data_path, 'contracts.pkl'), 'wb') as f:
        pickle.dump(data, f)
    log.info("清理完成,即将退出系统")
    exit(0)

def custom_sink(message):
    event_engine.put(type=MsgType.ON_LOG,data=message)

def custon_filter(record):
    module_path = record.get('name', '')
    return 'qts.trader.gateway' in module_path

async def main(acctId: str = None):
    add_custom_sink(custom_sink,custon_filter,level=INFO)
    acct_conf = conf_dao.get_acct_config(acctId)
    if acct_conf is None:
        log.error(f"账户 {acctId} 不存在")
        return       
    log.info(f"启动账户: {acctId}")


    trader_inst = TraderInst(acct_conf)
    trader_inst.start()
    await asyncio.sleep(500)

# 注册信号处理器
signal.signal(signal.SIGINT, handle_exit)  # 捕捉 Ctrl+C
signal.signal(signal.SIGTERM, handle_exit)



