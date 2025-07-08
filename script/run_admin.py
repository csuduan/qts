import os
import json
import uvicorn
from qts.common import init

config_file=os.path.join(os.path.dirname(os.path.abspath(__file__)), "conf_admin.json")
if not os.path.exists(config_file):
    exit(-1)
with open(config_file, "r") as f:
    configs = json.load(f)
init(configs)


if __name__ == '__main__':
    from qts.admin.app import app
    from qts.common.config import config
 
    uvicorn.run(app,host='0.0.0.0',port=config.get_config('server_port'))
