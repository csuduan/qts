package org.mts.admin.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.mts.admin.entity.Page;
import org.mts.admin.entity.Response;
import org.mts.admin.service.AgentService;
import org.mts.common.model.acct.AcctDetail;
import org.mts.common.model.acct.AcctInfo;
import org.mts.common.model.Enums;
import org.mts.common.model.rpc.Message;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "账户管理")
@RestController
@RequestMapping(value = "/v1/acct")
@Slf4j
public class AcctController {
    @Autowired
    private AgentService agentService;

    @ApiOperation(value = "获取账户列表")
    @GetMapping(value = "/list")
    public Response<Page<AcctInfo>> getAcctList(){
        Response<Page<AcctInfo>> response=new Response<>();
        //List<AcctInfo> acctInfos=agentService.getAgents();

        Page<AcctInfo> res=new Page<>();
        //res.setList(acctInfos);
        //res.setTotal(acctInfos.size());
        response.setData(res);
        return response;
    }

    @ApiOperation(value = "获取账户明细")
    @GetMapping(value = "/detail")
    public Response<AcctDetail> getAcctDetail(String acctId){
        Response<AcctDetail> response=new Response<>();
        return response;
    }

    @ApiOperation(value = "账户操作")
    @PostMapping(value = "/operate")
    public Response<Boolean> operateAcct(Enums.MSG_TYPE type,String acctId,String data){
        Response<Boolean> response=new Response<>();
        response.setData(agentService.acctOperate(acctId,type,data));
        return response;
    }
}
