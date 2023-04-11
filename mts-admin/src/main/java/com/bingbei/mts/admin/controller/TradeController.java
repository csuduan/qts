package com.bingbei.mts.admin.controller;

import com.alibaba.fastjson.JSON;
import com.bingbei.mts.admin.entity.Message;
import com.bingbei.mts.admin.entity.Operate;
import com.bingbei.mts.admin.manager.TradeManager;
import com.bingbei.mts.common.entity.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(value = "交易管理")
@RestController
@RequestMapping(value = "/v1/trade")
@Slf4j
public class TradeController {
    @Autowired
    private TradeManager tradeEngineManager;

    @ApiOperation(value = "交易操作")
    @PostMapping(value = "/operate")
    public Response<Boolean> engineOperate(@RequestParam String acctId, @RequestParam Operate.Cmd cmd, @RequestBody Map<String,Object> data){
        Response<Boolean> response=new Response<>();
        Message message=new Message();
        message.setType(cmd);
        message.setData(JSON.toJSONString(data));
        response.setData(tradeEngineManager.tradeEngineOperate(acctId,message));
        return response;
    }
}
