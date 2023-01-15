package com.bingbei.mts.trade.factory;

import com.bingbei.mts.adapter.ctp.CtpMdGateway;
import com.bingbei.mts.adapter.ctp.CtpTdGateway;
import com.bingbei.mts.common.entity.Account;
import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.gateway.MdGateway;
import com.bingbei.mts.common.gateway.TdGateway;
import com.bingbei.mts.common.service.FastEventEngineService;
import org.springframework.stereotype.Component;

@Component
public class GatwayFactory {
    private FastEventEngineService fastEventEngineService;
    public GatwayFactory(FastEventEngineService fastEventEngineService){
        this.fastEventEngineService=fastEventEngineService;
    }

    public TdGateway createTdGateway(Account account){
        if("CTP".equals(account.getLoginInfo().getTdType()))
            return new CtpTdGateway(fastEventEngineService,account.getLoginInfo());
        return null;
    }

    public MdGateway createMdGateway(MdInfo mdInfo){
        return new CtpMdGateway(fastEventEngineService,mdInfo);
    }
}
