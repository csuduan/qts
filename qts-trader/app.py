import asyncio
import os
from typing import List

from config import get_setting
from core.admin_engine import AdminEngine
from core.trade_engine import TradeEngine
from core import  trade_engine, admin_engine


async def main():

    acct_confs: List = get_setting('acct_confs')
    sim_conf = next((conf for conf in acct_confs if conf['id'] == 'sim'), None)
    trade_engine.start(sim_conf)

    admin_engine.start("tcp://*:4101", "tcp://*:4102")
    await asyncio.sleep(600)


if __name__ == '__main__':
    asyncio.run(main())
