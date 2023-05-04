package org.mts.admin.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.util.BeanUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.mts.admin.entity.Enums;
import org.mts.admin.entity.Message;
import org.mts.admin.entity.Page;
import org.mts.admin.entity.Response;
import org.mts.admin.entity.acct.AcctDetail;
import org.mts.admin.entity.acct.AcctInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Api(value = "账户管理")
@RestController
@RequestMapping(value = "/v1/acct")
@Slf4j
public class AcctController {

    @ApiOperation(value = "获取账户列表")
    @GetMapping(value = "/list")
    public Response<Page<AcctInfo>> getAcctList(){
        AcctInfo acctInfo=new AcctInfo();
        acctInfo.setGroup("SIM");
        acctInfo.setId("DQ");
        acctInfo.setName("DQ-SIM");
        acctInfo.setUser("048997");
        acctInfo.setAcctStatus(Enums.ACCT_STATUS.UNKNOW);
        acctInfo.setApiStatus(Enums.API_STATUS.UNKNOW);
        acctInfo.setBalance(BigDecimal.valueOf(10000000));
        acctInfo.setMv(BigDecimal.valueOf(10000000));
        acctInfo.setBalanceProfit(BigDecimal.valueOf(1000));
        acctInfo.setCloseProfit(BigDecimal.valueOf(1000));
        acctInfo.setMargin(BigDecimal.valueOf(1000));
        acctInfo.setEnable(true);

        List<AcctInfo> acctInfos=new ArrayList<>();
        acctInfos.add(acctInfo);

        for(int i=0;i<=15;i++){
            AcctInfo acctInfo1=new AcctInfo();
            BeanUtils.copyProperties(acctInfo,acctInfo1);
            acctInfo1.setId("TEST"+i);
            acctInfo1.setName("TEST-SIM-"+i);
            acctInfo1.setEnable(false);
            acctInfos.add(acctInfo1);
        }



        Page<AcctInfo> res=new Page<>();
        res.setList(acctInfos);
        res.setTotal(acctInfos.size());
        Response<Page<AcctInfo>> response=new Response<>();
        response.setData(res);
        return response;
    }

    @ApiOperation(value = "获取账户明细")
    @GetMapping(value = "/detail")
    public Response<AcctDetail> getAcctDetail(String acctId){
        Response<AcctDetail> response=new Response<>();
        return response;
    }
}
