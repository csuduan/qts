import requests
import json
from qts.common import get_logger
log = get_logger(__name__)

base_url = "http://8.130.149.67:8080/wecomchan?sendkey=my_key&msg_type=text"
def send_wechat(msg:str):
    data = {
        "msg_type": "text",
        "msg":msg
    }
    header = {
        "Content-Type":"application/json"
    }
    rsp = requests.post(base_url,data=json.dumps(data),headers=header)
    if rsp.status_code != 200 or rsp.json()['errcode']!=0:
        log.error(f"发送微信消息失败：{msg},{rsp.text}")
