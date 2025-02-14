from typing import List,Dict
from model.object import AccountData, AcctInfo,AcctConf,Position,ContractData,TradeData,TickData,OrderData,SubscribeRequest
from core.event.event import EventEngine, EventType,Event
from gateway import create_gateway, BaseGateway
from qts.model.message import MsgType,MsgHandler,Message
from qts.tcp.server import TcpServer




class AcctInst():
    def __init__(self) -> None:
        self.gateway: BaseGateway = None
        self.acct_info: AcctInfo = None
        self.position_map: Dict[str,Position] = {}
        self.trade_map: Dict[str,TradeData] = {}
        self.order_map: Dict[str,OrderData] = {}
        self.quote_map: Dict[str,TickData] = {}
        self.contracts_map: Dict[str, ContractData] = {}
        self.tcp_server = None

    def start(self,config:AcctConf):
        """启动"""
        self.acct_conf = config
        self.acct_info = AcctInfo(id=config.id,group=config.group,name=config.name,enable=config.enable,conf=config)

        self.event_engine = EventEngine()
        self.event_engine.start()

        self.acct_event_handler = AcctEventHandler(self,self.event_engine)

        self.gateway = create_gateway('ctp', self.event_engine, self.acct_info)
        self.gateway.connect()

    def add_tcp_server(self,tcp_server:TcpServer):
        self.tcp_server = tcp_server

    def push_msg(self,type,data):
        self.tcp_server.push( Message(type=type,data=data))
        

class AcctEventHandler():
    def __init__(self,acct_inst:AcctInst,event_engine:EventEngine) -> None:
        self.acct_inst = acct_inst
        event_engine.register(EventType.EVENT_CONTRACT, self.on_contract)
        event_engine.register(EventType.EVENT_POSITIONS, self.on_position)
        event_engine.register(EventType.EVENT_TICK, self.on_tick)
        event_engine.register(EventType.EVENT_ACCOUNT, self.on_account)
        event_engine.register(EventType.EVENT_STATUS, self.on_status)

    def on_status(self,event:Event) :
        self.acct_inst.push_msg(MsgType.ON_ACCT_INFO,self.acct_inst.acct_info)

    def on_contract(self,event:Event) :
        self.acct_inst.contracts_map= event.data
    
    def on_position(self,event:Event) :
        positions :List[Position] = event.data
        self.acct_inst.position_map.clear()
        for position in positions:
            self.acct_inst.position_map[position.id] = position
        
        symbols = {(p.symbol, p.exchange) for p in self.acct_inst.position_map.values()}
        for symbol_pair in symbols:
            self.acct_inst.gateway.subscribe(SubscribeRequest(symbol=symbol_pair[0],exchange=symbol_pair[1]))

        self.acct_inst.push_msg(MsgType.ON_POSITION,list(self.acct_inst.position_map.values()))
    

    def on_trade(self,event:Event) :
        trade:TradeData = event.data
        self.acct_inst.trade_map[trade.tradeid] = trade
        self.acct_inst.push_msg(MsgType.ON_TRADE,trade)
        
    def on_tick(self,event:Event) :
        tick:TickData = event.data
        self.acct_inst.quote_map[tick.symbol] = tick
        self.acct_inst.tcp_server.push( Message(type=MsgType.ON_TICK,data=tick))

    def on_order(self,event:Event) :
        pass
    def on_account(self,event:Event) :
        account:AccountData = event.data
        self.acct_inst.acct_info.balance = account.balance
        self.acct_inst.acct_info.available = account.available
        self.acct_inst.acct_info.frozen = account.frozen
        self.acct_inst.acct_info.margin_rate = account.margin_rate
        self.acct_inst.acct_info.hold_profit = account.hold_profit
        self.acct_inst.acct_info.close_profit = account.close_profit
        self.acct_inst.push_msg(MsgType.ON_ACCT_INFO,self.acct_inst.acct_info)
    

    
    
