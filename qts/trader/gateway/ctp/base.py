import sys
import os
from zoneinfo import ZoneInfo

from qts.common.constant import *
from qts.common import get_config,get_logger
#from openctp_ctp.thosttraderapi import *
#from openctp_ctp.thostmduserapi import *

from openctp_ctp import thosttraderapi as tdapi

log = get_logger(__name__)

from qts.common.object import ( 
        AcctConf,
        ContractData,
        AcctDetail,
        SubscribeRequest,
        OrderRequest,
        OrderCancel,
        OrderData,
        Position,
        StatusData,
        TickData,
        AccountData,
        TradeData,
        ProductData
    )

# 其他常量
MAX_FLOAT = sys.float_info.max                  # 浮点数极限值
CHINA_TZ = ZoneInfo("Asia/Shanghai")       # 中国时区

STATUS_CTP2VT = {
    tdapi.THOST_FTDC_OAS_Submitted: Status.SUBMITTING,
    tdapi.THOST_FTDC_OAS_Accepted: Status.SUBMITTING,
    tdapi.THOST_FTDC_OAS_Rejected: Status.REJECTED,
    tdapi.THOST_FTDC_OST_NoTradeQueueing: Status.NOTTRADED,
    tdapi.THOST_FTDC_OST_PartTradedQueueing: Status.PARTTRADED,
    tdapi.THOST_FTDC_OST_AllTraded: Status.ALLTRADED,
    tdapi.THOST_FTDC_OST_Canceled: Status.CANCELLED
}

DIRECTION_VT2CTP = {
    Direction.BUY: tdapi.THOST_FTDC_D_Buy,
    Direction.SELL: tdapi.THOST_FTDC_D_Sell
}
DIRECTION_CTP2VT = {v: k for k, v in DIRECTION_VT2CTP.items()}

POS_DIRECTION_VT2CTP = {
    PosDirection.LONG: tdapi.THOST_FTDC_PD_Long,
    PosDirection.SHORT: tdapi.THOST_FTDC_PD_Short
}   
POS_DIRECTION_CTP2VT = {v: k for k, v in POS_DIRECTION_VT2CTP.items()}

OFFSET_VT2CTP = {
    Offset.OPEN: tdapi.THOST_FTDC_OF_Open,
    Offset.CLOSE: tdapi.THOST_FTDC_OFEN_Close,
    Offset.CLOSETODAY: tdapi.THOST_FTDC_OFEN_CloseToday,
    Offset.CLOSEYESTERDAY: tdapi.THOST_FTDC_OFEN_CloseYesterday,    
}
OFFSET_CTP2VT = {v: k for k, v in OFFSET_VT2CTP.items()}

EXCHANGE_CTP2VT = {
    "CFFEX": Exchange.CFFEX,
    "SHFE": Exchange.SHFE,
    "CZCE": Exchange.CZCE,
    "DCE": Exchange.DCE,
    "INE": Exchange.INE,
    "GFEX": Exchange.GFEX,
}

PRODUCT_CTP2VT = {
    tdapi.THOST_FTDC_PC_Futures: ProductType.FUTURES,
    tdapi.THOST_FTDC_PC_Options: ProductType.OPTION,
    tdapi.THOST_FTDC_PC_SpotOption: ProductType.OPTION,
    tdapi.THOST_FTDC_PC_Combination: ProductType.SPREAD
}

# 期权类型映射
OPTIONTYPE_CTP2VT: dict[str, OptionType] = {
    tdapi.THOST_FTDC_CP_CallOptions: OptionType.CALL,   
    tdapi.THOST_FTDC_CP_PutOptions: OptionType.PUT
}

ACTIVE_STATUSES=[Status.SUBMITTING, Status.NOTTRADED, Status.PARTTRADED]



def adjust_price(price: float) -> float:
    """将异常的浮点数最大值（MAX_FLOAT）数据调整为0"""
    if price == MAX_FLOAT:
        price = 0
    return price


def get_data_path(folder_name: str):
    data_path = get_config('data_path')
    folder_path = os.path.join(data_path, folder_name)
    if not os.path.exists(folder_path):
        os.makedirs(folder_path)
    return folder_path