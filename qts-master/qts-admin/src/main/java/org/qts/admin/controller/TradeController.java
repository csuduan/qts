package org.qts.admin.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(value = "交易管理")
@RestController
@RequestMapping(value = "/v1/trade")
@Slf4j
public class TradeController {
    //@Autowired
    //private TradeManager tradeEngineManager;

//    @ApiOperation(value = "交易操作")
//    @PostMapping(value = "/operate")
//    public Response<Boolean> engineOperate(@RequestParam String acctId, @RequestParam Enums.MSG cmd, @RequestBody Map<String,Object> data){
//        Response<Boolean> response=new Response<>();
//        Message message=new Message();
//        message.setType(cmd);
//        message.setActId(acctId);
//        message.setJson(JSON.toJSONString(data));
//        response.setData(tradeEngineManager.tradeEngineOperate(acctId,message));
//        return response;
//    }
}
