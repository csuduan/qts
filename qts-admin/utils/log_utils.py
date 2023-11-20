import logging
from logging.handlers import RotatingFileHandler
import os
from apscheduler.schedulers.background import BackgroundScheduler
from .config_utils import configs

log_path =configs['log_path']
if not os.path.exists(log_path):
    os.mkdir(log_path)
handler = RotatingFileHandler(filename=os.path.join(log_path,'qts-admin.log'),
                                maxBytes=512 * 1024 * 1024, backupCount=20)
handler.setLevel(logging.INFO)


def get_logger(__name__):
    logger = logging.getLogger(__name__)
    logger.setLevel(level=logging.INFO)
    if not os.path.exists("/opt/logs/py-aas"):
        os.mkdir("/opt/logs/py-aas")
    formatter = logging.Formatter('%(asctime)s - %(name)s %(levelname)s - %(message)s')
    console = logging.StreamHandler()
    console.setLevel(level=logging.INFO)
    handler.setFormatter(formatter)
    console.setFormatter(formatter)
    logger.addHandler(handler)
    logger.addHandler(console)
    return logger


def cron_start():
    scheduler = BackgroundScheduler()
    scheduler.add_job(handler.doRollover, 'cron', hour=23, minute=59, second=59)
    scheduler.start()