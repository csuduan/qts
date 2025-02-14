import os
from .logger_utils import LoggerUtils
logger_utils = LoggerUtils()

def get_logger(name:str):
    return logger_utils.get_logger(name)
