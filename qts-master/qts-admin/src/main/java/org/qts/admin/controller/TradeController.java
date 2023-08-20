package org.qts.admin.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.qts.admin.service.TradeService;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;
import org.qts.common.entity.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "账户交易")
@RestController
@RequestMapping(value = "/v1/trade")
@Slf4j
public class TradeController {
    @Autowired
    private TradeService tradeEngineManager;

    @ApiOperation(value = "交易操作")
    @PostMapping(value = "/operate")
    public Response<Boolean> engineOperate(@RequestParam String acctId, @RequestParam Enums.ACCT_CMD cmd, @RequestBody Map<String,Object> data){
        Response<Boolean> response=new Response<>();
        Message message=new Message();
//        message.setType(cmd);
//        message.setActId(acctId);
//        message.setJson(JSON.toJSONString(data));
//        response.setData(tradeEngineManager.tradeEngineOperate(acctId,message));
        return response;
    }
}
