from abc import ABC, abstractmethod
from qts.common.object import  TickData,BarData,OrderData,TradeData

class BaseStrategy(ABC):
    def __init__(self):
        pass

    @abstractmethod
    def on_tick(self, tick:TickData):
        pass

    @abstractmethod
    def on_bar(self, bar:BarData):
        pass

    @abstractmethod
    def on_order(self, order:OrderData):
        pass

    @abstractmethod
    def on_trade(self,trade:TradeData):
        pass

    def get_config(self, key:str):
        pass

    def open(self,symbol:str,price:float,volume:float):
        pass

    def close(self,symbol:str,price:float,volume:float):
        pass

    def cancel(self,order_id:str):
        pass