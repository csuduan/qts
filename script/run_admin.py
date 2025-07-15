import os
import uvicorn

os.environ['QTS_CONFIG'] = os.path.join(os.path.dirname(os.path.abspath(__file__)), "conf_admin.json")

if __name__ == '__main__':
    from qts.admin.app import app 
    from qts.common import get_config

    uvicorn.run(app,host='0.0.0.0',port=get_config('server_port'))
