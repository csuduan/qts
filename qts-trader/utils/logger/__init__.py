import os
from .logger_utils import LoggerUtils
from config import get_setting
logger_utils = LoggerUtils()
#初始化日志
log_path = get_setting("log_path")
if not os.path.exists(log_path):
    os.makedirs(log_path)
log_file = os.path.join(log_path, "pytrader.log")
logger_utils.init(log_file)