import argparse
import asyncio
import os
import json
from qts.common import init

parser = argparse.ArgumentParser(description="parse args")
parser.add_argument("--acctId", help="账户ID", default='sim')
acctId = parser.parse_args().acctId

config_file=os.path.join(os.path.dirname(os.path.abspath(__file__)), "conf_trader.json")
if not os.path.exists(config_file):
    exit(-1)
with open(config_file, "r") as f:
    configs = json.load(f)
configs['app_name']= configs['app_name']+'-'+acctId
init(configs)

if __name__ == '__main__':  
    from qts.trader.app import main
    asyncio.run(main(acctId))