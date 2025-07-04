from typing import Dict, Any
import os,json

class Config():
    def __init__(self):
        pass
    def init(self,configs:dict):
        self.configs = configs

    def get_config(self,key:str):
        if key not in self.configs:
            return None
        return self.configs[key]

    def set_config(self,key:str,value:Any):        
        self.configs[key] = value

    def get_all_configs(self):
        return self.configs
