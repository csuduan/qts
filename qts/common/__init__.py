from .config import config

def get_config(key:str,default:any=None):
    return config.get_config(key,default)

def set_config(key:str,value:str):
    return config.set_config(key,value)

def get_logger( __name__):   
    from .logger import logger
    return logger

__all__ = [
    "set_conf",
    "get_conf",
    "get_logger"
    ]


