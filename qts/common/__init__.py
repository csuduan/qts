from .config import config
from .logger import logger


def get_config(key:str):
    return config.get_config(key)

def set_config(key:str,value:str):
    return config.set_config(key,value)

def get_logger( __name__):   
    return logger

__all__ = [
    "set_conf",
    "get_conf",
    "get_logger"
    ]


