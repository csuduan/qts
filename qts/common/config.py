from typing import Dict, Any
import os,json, sys

class Config():
    def __init__(self):
        self.configs = None
        self.inited = False
    
    def get_config(self,key:str,default:Any=None):
        if not self.inited:
            self.__init_config()
        if key not in self.configs:
            return default
        return self.configs[key]

    def set_config(self,key:str,value:Any): 
        if not self.inited:
            self.__init_config()
        self.configs[key] = value

    def __init_config(self):
        config_file = os.getenv('QTS_CONFIG', os.path.join(os.path.dirname(os.path.abspath(__file__)), "conf.json"))
        if not os.path.exists(config_file):
            exit(-1)
        with open(config_file, "r",encoding="utf-8") as f:
            self.configs = json.load(f)
        self.inited = True

config = Config()