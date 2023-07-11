//
// Created by 段晴 on 2022/6/9.
//

#ifndef MTS_CORE_TASKSCHEDULER_H
#define MTS_CORE_TASKSCHEDULER_H

#include "cron.hpp"
#include "timer.hpp"
#include "string"
#include "map"
#include "fmtlog/fmtlog.h"

struct TaskInfo{
    std::string name;
    std::string cron;
    std::time_t last;
    std::time_t next;
    std::function<void()> task;
    bool ready= false;//就绪状态(等待执行)
};


class TaskScheduler {
private:
    std::map<std::string ,TaskInfo> taskMap;
    bool reset(TaskInfo & taskInfo){
        try
        {
            auto cronInfo = cron::make_cron((std::string_view)taskInfo.cron);
            std::time_t now = std::time(0);
            std::time_t next = cron::cron_next(cronInfo, now);

            taskInfo.last=now;
            taskInfo.next=next;
            taskInfo.ready= false;
        }
        catch (cron::bad_cronexpr const & ex)
        {
            loge("parse cron error {} {}",taskInfo.name,taskInfo.cron);
            return false;
        }
        return true;
    }

    void scanTask(){
        while (true){
            for (auto & [name,taskInfo]: this->taskMap) {
                std::time_t now = std::time(0);
                if(!taskInfo.ready && difftime(now,taskInfo.next)>0){
                    //执行任务
                    taskInfo.ready=true;
                    auto task= &taskInfo;
                    //task->task();
                    //this->reset(taskInfo);
                    //异步执行
                    std::thread workThread([this,task](){
                        task->task();
                        this->reset(*task);
                    });
                    workThread.detach();
                }
            }
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
    }

public:
    void addTask(std::string name,std::string cron,std::function<void()> task){
        TaskInfo taskInfo ;
        taskInfo.name=name;
        taskInfo.cron=cron;
        taskInfo.task=task;

        if(this->reset(taskInfo))
            //this->taskMap.insert(make_pair(taskInfo.name,std::move(taskInfo)));
            this->taskMap[taskInfo.name] = std::move(taskInfo);
        logi("add task:{}  success",taskInfo.name,taskInfo.cron);
    }

    void start(){
        std::thread scanThread([this](){
            this->scanTask();
        });
        scanThread.detach();
    }




};



#endif //MTS_CORE_TASKSCHEDULER_H
