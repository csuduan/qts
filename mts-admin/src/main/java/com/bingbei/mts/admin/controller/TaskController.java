package com.bingbei.mts.admin.controller;

import com.bingbei.mts.admin.entity.JobInfo;
import com.bingbei.mts.common.entity.Response;
import com.bingbei.mts.common.exception.BizException;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Api(tags="任务管理")
@Slf4j
@RestController
@RequestMapping(value = "/v1/task")
public class TaskController {
    @Autowired
    private Scheduler scheduler;

    @GetMapping(path = "/list")
    public Response<List<JobInfo>> getTasks(){
        List<JobInfo> jobInfos=new ArrayList<>();
        try {
            Set<JobKey> jobKeySet = scheduler.getJobKeys(GroupMatcher.anyGroup());
            for(JobKey jobKey:jobKeySet){
                JobInfo jobInfo=new JobInfo();
                jobInfo.setGroup(jobKey.getGroup());
                jobInfo.setName(jobKey.getName());
                JobDetail jobDetail=scheduler.getJobDetail(jobKey);
                jobInfo.setDesc(jobDetail.getDescription());
                List<CronTrigger> triggers = (List<CronTrigger>) scheduler.getTriggersOfJob(jobKey);
                jobInfo.setCron(triggers.get(0).getCronExpression());
                jobInfo.setNextFireTime(triggers.get(0).getNextFireTime());
                jobInfo.setBeforFireTime(triggers.get(0).getPreviousFireTime());
                Trigger.TriggerState triggerState=scheduler.getTriggerState(triggers.get(0).getKey());
                jobInfo.setStatus(triggerState.name());
                jobInfos.add(jobInfo);

            }
        }catch (Exception ex){
            throw new BizException(ex);
        }
        Response<List<JobInfo>> response=new Response<>();
        response.setData(jobInfos);
        return response;
    }
    @GetMapping(path = "/trig")
    public Response<Boolean> trigJob(@RequestParam("name") String jobName,@RequestParam("group") String jobGroup){
        try {
            JobKey jobKey=new JobKey(jobName,jobGroup);
            scheduler.triggerJob(jobKey);
        }catch (Exception ex){
            throw new BizException(ex);
        }
        Response<Boolean> response=new Response<>();
        response.setData(true);
        return response;
    }
    @GetMapping(path = "/pause")
    public Response<Boolean> pauseJob(@RequestParam("name") String jobName,@RequestParam("group") String jobGroup){
        try {
            JobKey jobKey=new JobKey(jobName,jobGroup);
            scheduler.pauseJob(jobKey);
        }catch (Exception ex){
            throw new BizException(ex);
        }
        Response<Boolean> response=new Response<>();
        response.setData(true);
        return response;
    }
    @GetMapping(path = "/resume")
    public Response<Boolean> resumeJob(@RequestParam("name") String jobName,@RequestParam("group") String jobGroup){
        try {
            JobKey jobKey=new JobKey(jobName,jobGroup);
            scheduler.resumeJob(jobKey);
        }catch (Exception ex){
            throw new BizException(ex);
        }
        Response<Boolean> response=new Response<>();
        response.setData(true);
        return response;
    }
}
