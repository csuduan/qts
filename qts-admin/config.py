from typing import Dict, Any
import json
import os
from qts import init
from qts.config import config

SETTINGS: Dict[str, Any] = {
    "app_name": "qts",
    "server_port": 8084,
    "api_path": "/opt/qts/api",
    "data_path": "/opt/qts/data",
    "log_path": "/opt/qts/log",
    "db_url": "sqlite:////opt/dev/qts/qts.db",
}
cur_path = os.path.dirname(os.path.abspath(__file__))
with open(os.path.join(cur_path, "config.json"), "r") as f:
    configs = json.load(f)
    for k, v in configs.items():
        SETTINGS[k] = v


init(SETTINGS)

def get_setting(key: str) -> Any:
    return config.get_config(key)
