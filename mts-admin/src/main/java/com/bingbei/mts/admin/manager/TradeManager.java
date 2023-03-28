package com.bingbei.mts.admin.manager;

import com.alibaba.fastjson.JSON;
import com.bingbei.mts.admin.entity.Operate;
import com.bingbei.mts.admin.entity.OrderReq;
import com.bingbei.mts.admin.entity.config.TradeConfig;
import com.bingbei.mts.common.entity.Account;
import com.bingbei.mts.common.entity.LoginInfo;
import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.utils.ResourceUtil;
import com.bingbei.mts.trade.engine.TradeEngine;
import com.bingbei.mts.trade.engine.TradeEngineFactory;
import com.bingbei.mts.trade.strategy.StrategySetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@DependsOn("springUtils")
public class TradeManager {
    @Autowired
    private TradeEngineFactory tradeEngineFactory;

    @PostConstruct
    public  void init() throws Exception{
        //Load config
        String txt=ResourceUtil.getFileJson("trade.json");
        TradeConfig tradeConfig= JSON.parseObject(txt,TradeConfig.class);
        Map<String,MdInfo> mdInfoMap=new HashMap<>();
        if(tradeConfig.getMds()!=null){
            tradeConfig.getMds().forEach(x->{
                MdInfo mdInfo=new MdInfo(x.getMdId(),x.getAddress());
                mdInfoMap.put(mdInfo.getId(),mdInfo);
            });
        }

        //create tradeEngine
        if(tradeConfig.getTradeEngines()!=null){
            tradeConfig.getTradeEngines().forEach(x->{
                var mdInfo=mdInfoMap.get(x.getMdId());
                tradeEngineFactory.createTradeEngine(x.getEngineId(),mdInfo);
                //create account
                x.getAccounts().forEach(accountConfig->{
                    Account account=new Account();
                    account.setId(accountConfig.getId());
                    account.setName(accountConfig.getName());
                    account.setLoginInfo(new LoginInfo(accountConfig.getId(),accountConfig.getUser(),accountConfig.getTdAddress()));
                    tradeEngineFactory.createAccount(x.getEngineId(),account);
                });
            });
        }

        //创建策略
        String strategyTxt=ResourceUtil.getFileJson("strategy.json");
        List<StrategySetting> strategySettings= JSON.parseArray(strategyTxt, StrategySetting.class);
        strategySettings.forEach(x->{
            TradeEngine tradeEngine=this.tradeEngineFactory.getTradeEngineByAccount(x.getAccountId());
            if(tradeEngine!=null){
                tradeEngine.createStrategy(x);
            }
        });


    }
    public boolean accountOperate(String accountId, Operate.Account operate){
        var engine=tradeEngineFactory.getTradeEngineByAccount(accountId);
        switch (operate){
            case CONNECT -> engine.connect(accountId);
            case DISCONNECT -> engine.discount(accountId);
        }
        return true;
    }

    public boolean tradeEngineOperate(String engineId,Operate.TradeEngine operate){
        var engine=tradeEngineFactory.getTradeEngine(engineId);
        switch (operate){
            case CONNECT_MD -> engine.connnectMd();
            case DISCONNECT_MD -> engine.disconnectMd();
        }
        return true;
    }
    public String order(OrderReq orderReq){
        return null;
    }
}
