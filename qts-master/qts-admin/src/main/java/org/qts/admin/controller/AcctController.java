package org.qts.admin.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.qts.admin.entity.AcctDesc;
import org.qts.admin.manager.AcctManager;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;
import org.qts.common.entity.Response;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.acct.AcctInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public Response<List<AcctDesc>> getAcctInstList(){
        Response<List<AcctDesc>> response=new Response<>();
        response.setData(acctService.getAcctInstDescs());
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
        Message req = new Message(Enums.MSG_TYPE.EXIT);
        acctService.request(acctId,req);
        return response;
    }

    @ApiOperation(value = "账户详情")
    @GetMapping(value = "/inst/detail")
    public Response<AcctDetail> getAcctDetail(String acctId){
        Response<AcctDetail> response=new Response<>();
        response.setData(acctService.getAcctDetail(acctId));
        return response;
    }

    @ApiOperation(value = "发送通用命令")
    @PostMapping(value = "/inst/common-cmd")
    public Response<Message> sendCmd(@RequestParam String acctId, @RequestParam Enums.MSG_TYPE type, @RequestBody Map datas){
        Response<Message> response=new Response<>();
        Message req = new Message(type,datas);
        response.setData(acctService.request(acctId,req));
        return response;
    }


}
