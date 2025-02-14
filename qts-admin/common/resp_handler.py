import time,datetime
from fastapi import Request,Response
from .resp import resp_error
from qts.log import get_logger

logger = get_logger(__name__)

async def resp_handler(request:Request,call_next):

    start_time = time.perf_counter()
    start_timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    try:
        response = await call_next(request)
    except Exception as ex:
        response = resp_error(9999,str(ex))
    return response