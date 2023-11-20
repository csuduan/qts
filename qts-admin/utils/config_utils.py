import os,json
configs={}

cur_path = os.path.dirname(__file__)
with open(os.path.join(cur_path,'../config.json'),'r') as f:
    configs = json.load(f)