import datetime
import os, pickle, signal
from core.event.event import EventEngine, EventType
from gateway import create_gateway, BaseGateway
from utils.logger import logger_utils
from model.object import ContractData, AcctConf
from config import get_setting, add_setting

log = logger_utils.get_logger(__name__)


def handle_exit(signum, frame):
    log.info("接收到退出信号，正在清理...")
    # 写合约信息到缓存
    log.info("缓存合约信息...")
    contracts_map: dict[str, ContractData] = get_setting('contracts')
    data = {'date': datetime.date.today(), 'contracts': contracts_map}
    data_path = get_setting('data_path')
    with open(os.path.join(data_path, 'contracts.pkl'), 'wb') as f:
        pickle.dump(data, f)
    log.info("清理完成,即将退出系统")
    exit(0)


# 注册信号处理器
signal.signal(signal.SIGINT, handle_exit)  # 捕捉 Ctrl+C
signal.signal(signal.SIGTERM, handle_exit)


class TradeEngine():
    """交易引擎"""
    def __init__(self):
        """Constructor"""
        self.gateway: BaseGateway = None
        self.event_engine = EventEngine()
        self.event_engine.register(EventType.Event_STATUS, self.__on_event)
        self.event_engine.register(EventType.EVENT_CONTRACT, self.__on_event)
        self.event_engine.register(EventType.EVENT_TICK, self.__on_event)


        self.__load_cache()

    def __load_cache(self):
        #加载合约缓存
        data_path = get_setting('data_path')
        contracts_path = os.path.join(data_path, 'contracts.pkl')
        if os.path.exists(contracts_path):
            with open(contracts_path, 'rb') as f:
                data = pickle.load(f)
                contracts_map: dict[str, ContractData] = data['contracts']
                contracts_date: str = data['date']
                if contracts_date == datetime.date.today() and contracts_map is not None:
                    # 当日缓存过合约不信，则直接加载
                    add_setting('contracts', contracts_map)

    def __on_event(self, event):
        if event.type == EventType.EVENT_CONTRACT:
            contracts_map: dict[str, ContractData] = event.data
            add_setting('contracts', contracts_map)
        elif event.type == EventType.Event_STATUS:
            log.info(f"接收到状态事件:{event.data}")
        elif event.type == EventType.EVENT_TICK:
            #log.info(f"接收到tick事件:{event.data}")
            pass



    def start(self,acct_conf:AcctConf):
        """启动"""
        self.event_engine.start()
        self.gateway = create_gateway('ctp', self.event_engine, acct_conf)
        self.gateway.connect()
