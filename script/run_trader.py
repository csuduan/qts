import os
import argparse
import asyncio

parser = argparse.ArgumentParser(description="parse args")
parser.add_argument("--acctId", help="账户ID", default='DQ')
parser.add_argument("--env", help="环境", default='prd')
acctId = parser.parse_args().acctId
env = parser.parse_args().env

os.environ['QTS_CONFIG'] = os.path.join(os.path.dirname(os.path.abspath(__file__)), "conf_trader.json")

if __name__ == '__main__':  
    from qts.common import get_config,set_config
    set_config('log_name',get_config('app_name')+'-'+acctId)
    set_config('env',env)
    from qts.trader.app import main
    asyncio.run(main(acctId))