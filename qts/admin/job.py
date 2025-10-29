import datetime
import os
from .app import acct_mgr
from qts.common import get_logger,get_config
from qts.common.wecomm import send_wechat

log = get_logger(__name__)

def connect_api_job():
    log.info('触发任务：connect_api_job')
    for inst in acct_mgr.get_all_insts():
        if inst.inst_status :
            inst.connect_api()

def disconnect_api_job():
    log.info('触发任务：disconnect_api_job')
    for inst in acct_mgr.get_all_insts():
        if inst.inst_status :
            inst.disconnect_api()

def before_open_job():
    log.info('触发任务：before_open_job')
    for inst in acct_mgr.get_all_insts():
        if inst.inst_status :
            pass

def after_close_job():
    log.info('触发任务：after_close_job')
    for inst in acct_mgr.get_all_insts():
        if inst.inst_status :
            #inst.disconnect()
            #检查账户单日盈亏是否超过阈值0.5%
            profit_rate = inst.get_acct_detail().acct_info.profit_rate
            profit_rate_limit = 0.005
            if abs(profit_rate) > profit_rate_limit:
                send_wechat(f"账户{inst.get_acct_detail().acct_info.name}单日盈亏超过阈值{profit_rate_limit*1000}‰，当前盈亏率{profit_rate*1000}‰")
            pass

def export_pos_job():
    log.info('触发任务：export_pos_job')

    lines=[]
    for inst in acct_mgr.get_all_insts():
        acct_detail = inst.get_acct_detail()
        for pos in acct_detail.position_map.values():
            line = f"{acct_detail.acct_info.name},{acct_detail.acct_info.trading_day},{pos.symbol},{pos.td_volume},{pos.yd_volume}"
            lines.append(line)
    date = datetime.date.today().strftime("%Y%m%d")
    #export_path = f"{get_config('data_path')}/export/positions"
    export_file = f"{get_config('data_path')}/export/positions/position-{date}.csv"
    os.makedirs(os.path.dirname(export_file), exist_ok=True)
    with open(export_file, mode='w',encoding='gb2312') as f:
        f.write("账户,交易日期,合约代码,方向,今仓,昨仓\n")
        for line in lines:
            f.write(line+'\n')
    send_wechat(f"导出账户持仓完成！共{len(lines)}条数据")
    log.info(f"导出持仓数据到 {export_file}")
    
