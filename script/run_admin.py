import os
import uvicorn
from qts.common import init

cur_path = os.path.dirname(os.path.abspath(__file__))
init(os.path.join(cur_path, "conf_admin.json"))

if __name__ == '__main__':
    from qts.admin.app import app
    from qts.common.config import config
    uvicorn.run(app,host='0.0.0.0',port=config.get_config('server_port'))
