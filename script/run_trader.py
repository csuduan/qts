import os
import argparse
import asyncio


os.environ['QTS_CONFIG'] = os.path.join(os.path.dirname(os.path.abspath(__file__)), "conf_admin.json")

if __name__ == '__main__':  
    from qts.trader.app import main
    from qts.common import get_config,set_config

    parser = argparse.ArgumentParser(description="parse args")
    parser.add_argument("--acctId", help="账户ID", default='DQ')
    acctId = parser.parse_args().acctId
    #将账户ID添加到应用名称中
    set_config('app_name',get_config('app_name')+'-'+acctId)
    asyncio.run(main(acctId))