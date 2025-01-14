from typing import Dict, Any
import json
import os

SETTINGS: Dict[str, Any] = {
    "api_path": "/opt/qts/api",
    "data_path": "/opt/qts/data",
    "log_path": "/opt/qts/log"
}
cur_path = os.path.dirname(os.path.abspath(__file__))
with open(os.path.join(cur_path, "config.json"), "r") as f:
    configs = json.load(f)
    for k, v in configs.items():
        SETTINGS[k] = v


def get_settings(prefix: str = "") -> Dict[str, Any]:
    prefix_length: int = len(prefix)
    settings = {k[prefix_length:]: v for k, v in SETTINGS.items() if k.startswith(prefix)}
    return settings


def get_setting(key: str) -> Any:
    if key in SETTINGS:
        return SETTINGS[key]
    else:
        return None


def add_setting(key: str, value: Any):
    SETTINGS[key] = value
