package org.qts.admin.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.qts.admin.manager.AcctManager;
import org.qts.common.entity.Response;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.acct.AcctInst;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.AcctOpReq;
import org.qts.common.entity.Page;
import org.qts.common.entity.acct.AcctInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "账户管理")
@RestController
@RequestMapping(value = "/v1/acct")
@Slf4j
public class AcctController {
    @Autowired
    private AcctManager acctService;

    @ApiOperation(value = "账户配置列表")
    @GetMapping(value = "/conf/list")
    public Response<List<AcctConf>> getAcctConfList(){
        Response<List<AcctConf>> response=new Response<>();
        response.setData(acctService.getAcctConfs());
        return response;
    }

    @ApiOperation(value = "账户实例列表")
    @GetMapping(value = "/inst/list")
    public Response<List<AcctInst>> getAcctDetail(){
        Response<List<AcctInst>> response=new Response<>();
        return response;
    }

    @ApiOperation(value = "启动账户实例")
    @GetMapping(value = "/inst/start")
    public Response<Boolean> startInst(String acctId){
        Response<Boolean> response=new Response<>();
        //response.setData(agentService.acctOperate(req.getAcctId(),req.getType(),data));
        return response;
    }
    @ApiOperation(value = "停止账户实例")
    @GetMapping(value = "/inst/stop")
    public Response<Boolean> stopInst(String acctId){
        Response<Boolean> response=new Response<>();
        //response.setData(agentService.acctOperate(req.getAcctId(),req.getType(),data));
        return response;
    }
}
