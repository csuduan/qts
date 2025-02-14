import os
def init(configs:dict):
    # 初始化配置
    from .config import config
    config.init(configs)

    # 初始化日志
    from .log import logger_utils
    app_name = config.get_config("app_name")
    log_path = config.get_config("log_path")
    if not os.path.exists(log_path):
        os.makedirs(log_path)
    log_file = os.path.join(log_path, app_name+".log")
    logger_utils.init(log_file)


