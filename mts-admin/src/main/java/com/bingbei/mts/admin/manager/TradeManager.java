package com.bingbei.mts.admin.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bingbei.mts.admin.entity.config.TradeConfig;
import com.bingbei.mts.common.entity.Account;
import com.bingbei.mts.common.entity.LoginInfo;
import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.utils.ResourceUtil;
import com.bingbei.mts.trade.factory.TradeEngineFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TradeManager {
    @Autowired
    private TradeEngineFactory tradeEngineFactory;
    private Map<String,String> accountEngineMap=new HashMap<>();


    @PostConstruct
    public  void init() throws Exception{
        //Load config
        String txt=ResourceUtil.getFileJson("trade.json");
        TradeConfig tradeConfig= JSON.parseObject(txt,TradeConfig.class);
        Map<String,MdInfo> mdInfoMap=new HashMap<>();
        if(tradeConfig.getMdConfigs()!=null){
            tradeConfig.getMdConfigs().forEach(x->{
                MdInfo mdInfo=new MdInfo(x.getMdId(),x.getAddress());
                mdInfoMap.put(mdInfo.getId(),mdInfo);
            });
        }

        //create tradeEngine
        if(tradeConfig.getTradeEngineConfigs()!=null){
            tradeConfig.getTradeEngineConfigs().forEach(x->{
                var mdInfo=mdInfoMap.get(x.getMdId());
                tradeEngineFactory.createTradeEngine(x.getEngineId(),mdInfo);
            });
        }

        //create tradeEngine
        if(tradeConfig.getAccountConfigs()!=null){
            tradeConfig.getAccountConfigs().forEach(x->{
                Account account=new Account();
                account.setId(x.getId());
                account.setName(x.getName());
                account.setLoginInfo(new LoginInfo(x.getId(),x.getUser(),x.getTdAddress()));
                var tradeEngine=tradeEngineFactory.getTradeEngine(x.getGroup());
                if(tradeEngine!=null){
                    tradeEngine.addAccount(account);
                    accountEngineMap.put(account.getId(),tradeEngine.getId());
                }
                else
                    log.error("找不到分组{}对应的交易引擎",x.getGroup());
            });
        }

    }
    public boolean connect(String accountId){
        String engineId=accountEngineMap.get(accountId);
        var engine=tradeEngineFactory.getTradeEngine(engineId);
        engine.connect(accountId);
        return true;
    }
    public boolean disconnect(String accountId){
        return true;
    }
}
