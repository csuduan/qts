import os
import argparse

parser = argparse.ArgumentParser(description="parse args")
parser.add_argument("--env", help="环境", default='prd')
env = parser.parse_args().env

os.environ['QTS_CONFIG'] = os.path.join(os.path.dirname(os.path.abspath(__file__)), "conf_admin.json")

if __name__ == '__main__':
    from qts.common import get_config,set_config
    set_config('log_name',get_config('app_name'))
    set_config('env',env)

    import uvicorn
    from qts.admin.app import app 
    from qts.common.logger import Loggers
    #uvicorn.run(app,host='0.0.0.0',port=get_config('server_port'))
    uv_config = uvicorn.Config(app,host='0.0.0.0',port=get_config('server_port'))
    uv_server = uvicorn.Server(uv_config)
    Loggers.init_config()
    uv_server.run()
