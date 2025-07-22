from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.job import Job
from datetime import datetime
import importlib


class JobMgr():          
    def start(self,job_configs:dict):
        self.configs = job_configs
        self.scheduler = BackgroundScheduler(timezone="Asia/Shanghai")
        self.scheduler.start()
        for job in job_configs:
            cron = self.__parse_cron(job['cron'])
            self.scheduler.add_job(func=self.__get_func(job['func']), trigger='cron', name=job['name'], id=job['id'], **cron)

    def __parse_cron(self,cron_str:str):
        # 将cron表达式解析为APScheduler需要的参数
        minute, hour, day, month, day_of_week = cron_str.split()
        return {
            'minute': minute,
            'hour': hour,
            'day': day,
            'month': month,
            'day_of_week': day_of_week
        }

    def __get_func(self,func_str:str):
        module_path, func_name = func_str.rsplit('.', 1)
        module = importlib.import_module(module_path)
        return getattr(module, func_name)
    
    def get_jobs(self):
        job_list = []
        for config in self.configs:
            job:Job = self.scheduler.get_job(config['id'])
            if job:
                job_desc = config.copy()
                job_desc['status'] = 'paused' if not job.next_run_time else 'running'
                job_desc['next_time'] = str(job.next_run_time)
                job_list.append(job_desc) 
        return job_list
    
    def operate(self,id,action:str):
        job:Job = self.scheduler.get_job(id)
        if not job:
            raise Exception(f"找不到任务: {id}")

        if action == 'pause':
            job.pause()
        elif action == 'resume':
            job.resume()
        elif action == 'trigger':
            job.modify(next_run_time=datetime.now())
        else:
            raise Exception(f"未知操作: {action}")
