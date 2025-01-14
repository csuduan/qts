from model.object import AcctInfo, AcctDetail, PositionData, TradeData, OrderData
from .rpc import RpcHandler, MsgType
from . import trade_engine

handler = RpcHandler()


@handler.register_handler(MsgType.CONNECT)
def connect(req) -> bool:
    trade_engine.gateway.connect()
    return True


@handler.register_handler(MsgType.DISCONNECT)
def disconnect(req) -> bool:
    trade_engine.gateway.close()
    return True


@handler.register_handler(MsgType.GET_ACCT_INFO)
def get_acct_info(req) -> AcctInfo:
    acct_info = AcctInfo()
    return acct_info


@handler.register_handler(MsgType.GET_ACCT_DETAIL)
def get_acct_detail(req) -> AcctDetail:
    acct_detail = AcctDetail()
    return acct_detail


@handler.register_handler(MsgType.GET_POSITIONS)
def get_positions(req) -> list[PositionData]:
    pass


@handler.register_handler(MsgType.GET_TRADES)
def get_trades(req) -> list[TradeData]:
    pass


@handler.register_handler(MsgType.GET_ORDERS)
def get_orders(req) -> list[OrderData]:
    pass


@handler.register_handler(MsgType.SEND_ORDER)
def send_order(req) -> bool:
    pass


@handler.register_handler(MsgType.CANCEL_ORDER)
def cancel_order(req) -> bool:
    pass
