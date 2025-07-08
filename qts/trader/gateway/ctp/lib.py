import sys
from openctp_ctp.thosttraderapi import *
from openctp_ctp.thostmduserapi import *
from zoneinfo import ZoneInfo
 
from qts.common.model.object import (
        ContractData,
        AcctInfo
    )
from qts.common.model.constant import (
    Direction,
    PosDirection,
    Offset,
    Exchange,
    OrderType,
    Product,
    Status,
    OptionType
)
STATUS_CTP2VT = {
    THOST_FTDC_OAS_Submitted: Status.SUBMITTING,
    THOST_FTDC_OAS_Accepted: Status.SUBMITTING,
    THOST_FTDC_OAS_Rejected: Status.REJECTED,
    THOST_FTDC_OST_NoTradeQueueing: Status.NOTTRADED,
    THOST_FTDC_OST_PartTradedQueueing: Status.PARTTRADED,
    THOST_FTDC_OST_AllTraded: Status.ALLTRADED,
    THOST_FTDC_OST_Canceled: Status.CANCELLED
}

DIRECTION_VT2CTP = {
    Direction.BUY: THOST_FTDC_D_Buy,
    Direction.SELL: THOST_FTDC_D_Sell
}

DIRECTION_CTP2VT = {v: k for k, v in DIRECTION_VT2CTP.items()}

POS_DIRECTION_VT2CTP = {
    PosDirection.LONG: THOST_FTDC_PD_Long,
    PosDirection.SHORT: THOST_FTDC_PD_Short
}   
POS_DIRECTION_CTP2VT = {v: k for k, v in POS_DIRECTION_VT2CTP.items()}



OFFSET_VT2CTP = {
    Offset.OPEN: THOST_FTDC_OF_Open,
    Offset.CLOSE: THOST_FTDC_OFEN_Close,
    Offset.CLOSETODAY: THOST_FTDC_OFEN_CloseToday,
    Offset.CLOSEYESTERDAY: THOST_FTDC_OFEN_CloseYesterday,
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
    THOST_FTDC_PC_Futures: Product.FUTURES,
    THOST_FTDC_PC_Options: Product.OPTION,
    THOST_FTDC_PC_SpotOption: Product.OPTION,
    THOST_FTDC_PC_Combination: Product.SPREAD
}

# 其他常量
MAX_FLOAT = sys.float_info.max                  # 浮点数极限值
CHINA_TZ = ZoneInfo("Asia/Shanghai")       # 中国时区


# 期权类型映射
OPTIONTYPE_CTP2VT: dict[str, OptionType] = {
    THOST_FTDC_CP_CallOptions: OptionType.CALL,
    THOST_FTDC_CP_PutOptions: OptionType.PUT
}


