import asyncio
import os,sys,signal,pickle,datetime
import argparse
from typing import List

from config import get_setting
from qts.log import logger_utils

from core.acct_inst import AcctInst
from core.admin_engine import admin_engine
from qts.model.object import  ContractData,AcctConf
from qts.tcp.server import TcpServer


log = logger_utils.get_logger(__name__)


def handle_exit(signum, frame):
    log.info("接收到退出信号，正在清理...")
    # 写合约信息到缓存
    log.info("缓存合约信息...")
    contracts_map: dict[str, ContractData] = get_setting('contracts')
    data = {'date': datetime.date.today(), 'contracts': contracts_map}
    data_path = get_setting('data_path')
    with open(os.path.join(data_path, 'contracts.pkl'), 'wb') as f:
        pickle.dump(data, f)
    log.info("清理完成,即将退出系统")
    exit(0)

async def main(acctId: str = None):

    acct_confs: List = get_setting('acct_confs')
    conf = next((conf for conf in acct_confs if conf['id'] == acctId), None)
    log.info(f"启动账户: {conf['id']}")

    acct_conf: AcctConf = AcctConf(**conf)
    acct_inst = AcctInst()
    acct_inst.start(acct_conf)
    #trade_engine.start(acct_conf)
    admin_engine.start(acct_conf.tcp_port,acct_inst)
    await asyncio.sleep(600)


if __name__ == '__main__':
    # 注册信号处理器
    signal.signal(signal.SIGINT, handle_exit)  # 捕捉 Ctrl+C
    signal.signal(signal.SIGTERM, handle_exit)

    parser = argparse.ArgumentParser(description="parse args")
    parser.add_argument("--acctId", help="账户ID", default='sim')
    acctId = parser.parse_args().acctId
    asyncio.run(main(acctId))
