package com.bingbei.mts.admin.controller;


import com.bingbei.mts.admin.entity.Operate;
import com.bingbei.mts.common.entity.Response;
import com.bingbei.mts.common.entity.Tick;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "TestController")
@RestController
@RequestMapping(value = "/v1/test")
@Slf4j
public class TestController {
    @GetMapping(value = "/test/time")
    public Response<Boolean> testTime(){
        Response<Boolean> response=new Response<>();
        long t1=System.nanoTime();
        for (int i=0;i<1000;i++)
            System.nanoTime();
        long diff=System.nanoTime()-t1;
        log.info("==> System.nanoTime:{}ns",diff/1001);

        Tick tick=new Tick();
        t1=System.nanoTime();
        for (int i=0;i<1000;i++)
            tick=new Tick();
        diff=System.nanoTime()-t1;
        log.info("==> new Tick:{}ns",diff/1000);

        t1=System.nanoTime();
        for (int i=0;i<1000;i++)
            log.debug("{}",tick);
        diff=System.nanoTime()-t1;
        log.info("==> log.debug:{}ns",diff/1000);


        //response.setBody(tradeManager.tradeEngineOperate(engineId,operate));
        return response;
    }

}
