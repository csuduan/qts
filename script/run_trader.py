import argparse
import asyncio
import os
from qts.common import init
init(os.path.join(os.path.dirname(os.path.abspath(__file__)), "conf_admin.json"))

if __name__ == '__main__':
    from qts.trader.app import main
    parser = argparse.ArgumentParser(description="parse args")
    parser.add_argument("--acctId", help="账户ID", default='sim')
    acctId = parser.parse_args().acctId
    asyncio.run(main(acctId))