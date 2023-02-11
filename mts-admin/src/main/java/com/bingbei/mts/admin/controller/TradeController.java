package com.bingbei.mts.admin.controller;

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

    @ApiOperation(value = "启动账户连接")
    @GetMapping(value = "/account/connect")
    public Response<Boolean> connectAccount(String accountId){
        Response<Boolean> response=new Response<>();
        response.setBody(tradeManager.connect(accountId));
        return response;
    }
    @ApiOperation(value = "断开账户连接")
    @GetMapping(value = "/account/disconnect")
    public Response<Boolean> disconnectAccount(String accountId){
        Response<Boolean> response=new Response<>();
        response.setBody(tradeManager.disconnect(accountId));
        return response;
    }
    @ApiOperation(value = "断开账户连接")
    @GetMapping(value = "/account/order")
    public Response<String> order(OrderReq orderReq){
        Response<String> response=new Response<>();
        response.setBody(tradeManager.order(orderReq));
        return response;
    }



}
