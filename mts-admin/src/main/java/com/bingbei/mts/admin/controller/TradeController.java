package com.bingbei.mts.admin.controller;

import com.bingbei.mts.admin.entity.Operate;
import com.bingbei.mts.admin.entity.OrderReq;
import com.bingbei.mts.admin.manager.TradeManager;
import com.bingbei.mts.common.entity.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "TradeController")
@RestController
@RequestMapping(value = "/v1/trade")
@Slf4j
public class TradeController {
    @Autowired
    private TradeManager tradeManager;

    @ApiOperation(value = "账户操作")
    @GetMapping(value = "/account/operate")
    public Response<Boolean> connectAccount(String accountId, Operate.Account operate){
        Response<Boolean> response=new Response<>();
        response.setBody(tradeManager.accountOperate(accountId,operate));
        return response;
    }
    @ApiOperation(value = "交易引擎操作")
    @GetMapping(value = "/trade-engine/operate")
    public Response<Boolean> disconnectAccount(String engineId,Operate.TradeEngine operate){
        Response<Boolean> response=new Response<>();
        response.setBody(tradeManager.tradeEngineOperate(engineId,operate));
        return response;
    }

}
