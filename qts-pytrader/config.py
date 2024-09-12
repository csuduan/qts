from typing import Dict, Any

configs: Dict[str, Any]={

}

SETTINGS: Dict[str, Any] = {
    "api_path": "/opt/data/api"
}

def get_settings(prefix: str = "") -> Dict[str, Any]:
    prefix_length: int = len(prefix)
    settings = {k[prefix_length:]: v for k, v in SETTINGS.items() if k.startswith(prefix)}
    return settings

def get_setting(key:str) -> Any:
    return SETTINGS[key]