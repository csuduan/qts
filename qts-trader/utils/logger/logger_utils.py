import logging
from logging.handlers import RotatingFileHandler
from shutil import copyfile
import os
import time

from apscheduler.schedulers.background import BackgroundScheduler

class MyRotatingFileHandler(RotatingFileHandler):
    def doRollover(self):
        """
        Do a rollover, as described in __init__().
        """
        if self.stream:
            self.stream.close()
            self.stream = None
        if self.backupCount > 0:
            date = time.strftime('%Y-%m-%d')
            # print(datetime.datetime.now())
            for i in range(self.backupCount - 1, 0, -1):
                sfn = self.rotation_filename("%s_%s.%d" % (self.baseFilename, date, i))
                dfn = self.rotation_filename("%s_%s.%d" % (self.baseFilename, date, i + 1))
                if os.path.exists(sfn):
                    if os.path.exists(dfn):
                        os.remove(dfn)
                    os.rename(sfn, dfn)
            dfn = self.rotation_filename(self.baseFilename + "_" + date + ".1")
            if os.path.exists(dfn):
                os.remove(dfn)
            self.rotate(self.baseFilename, dfn)
        if not self.delay:
            self.stream = self._open()

    def rotate(self, source, dest):
        copyfile(source, dest)
        with open(source, 'wb'):
            pass

class LoggerUtils(object):
    def __init__(self):
        self.handler = None
        self.inited = False

    def init(self, filename: str, loglevel=logging.INFO):
        if self.inited:
            return
        self.inited = True
        self.handler = MyRotatingFileHandler(filename=filename,
                                             maxBytes=512 * 1024 * 1024, backupCount=20)
        self.handler.setLevel(loglevel)
        cur_file = os.path.abspath(filename)
        cur_dir = os.path.dirname(cur_file)
        if not os.path.exists(cur_dir):
            os.mkdir(cur_dir)

        scheduler = BackgroundScheduler()
        scheduler.add_job(self.handler.doRollover, 'cron', hour=23, minute=59, second=59)
        scheduler.start()

        logger = self.get_logger(__name__)
        logger.info('log inited,%s', filename)


    def get_logger(self, __name__):
        logger = logging.getLogger(__name__)
        logger.setLevel(level=logging.INFO)
        formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(name)s - %(filename)s:%(lineno)d - %(message)s')
        console = logging.StreamHandler()
        console.setLevel(level=logging.INFO)
        console.setFormatter(formatter)
        logger.addHandler(console)

        self.handler.setFormatter(formatter)
        logger.addHandler(self.handler)
        return logger
