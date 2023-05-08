package org.mts.admin.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.mts.admin.entity.Message;
import org.mts.admin.entity.Response;
import org.mts.common.model.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(value = "交易管理")
@RestController
@RequestMapping(value = "/v1/trade")
@Slf4j
public class TradeController {

    @ApiOperation(value = "交易操作")
    @PostMapping(value = "/operate")
    public Response<Boolean> engineOperate(@RequestParam String acctId, @RequestParam Enums.MSG_TYPE cmd, @RequestBody Map<String,Object> data){
        Response<Boolean> response=new Response<>();
        Message message=new Message();
        message.setType(cmd);
        message.setActId(acctId);
        message.setJson(JSON.toJSONString(data));
        //response.setData(tradeEngineManager.tradeEngineOperate(acctId,message));
        return response;
    }
}
