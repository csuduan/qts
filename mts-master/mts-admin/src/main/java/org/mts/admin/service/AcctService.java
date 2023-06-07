package org.mts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.admin.dao.ConfMapper;
import org.mts.admin.dao.SysMapper;
import org.mts.common.model.acct.AcctInfo;
import org.mts.common.model.acct.ServerInfo;
import org.mts.common.model.Enums;
import org.mts.common.model.conf.AcctConf;
import org.mts.common.model.event.MessageEvent;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.listener.ClientListener;
import org.mts.common.rpc.tcp.TcpClient;
import org.mts.common.utils.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * 交易代理服务
 */
@Service
@Slf4j
public class AcctService {
    @Autowired
    private ConfMapper confMapper;

    private Map<String, TcpClient> tcpClientMap =new HashMap<>();
    private Map<String, ServerInfo> serverInfoMap =new HashMap<>();

    private Map<String,AcctConf> acctConfMap=new HashMap<>();

    private Map<String,AcctInfo> acctInfoMap=new HashMap<>();


    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private TradeService tradeService;

    @Value("${server.name}")
    private String owner;


    @PostConstruct
    public void init(){
        //初始化
        var list =confMapper.getAcctConf(owner);
        var quoteConfs=confMapper.getQuoteConf();
        for(var acctConf :list){
            if(StringUtils.hasLength(acctConf.getQuotes())){
                for(String quote:acctConf.getQuotes().split(",")){
                    var opt=quoteConfs.stream().filter(q->q.getId().equals(quote)).findFirst();
                    if(opt.isPresent())
                        acctConf.getQuoteConfs().add(opt.get());
                }
            }
            acctConfMap.put(acctConf.getId(),acctConf);
            tradeService.startAcctClient(acctConf);
        }
    }


    public List<AcctInfo> getAcctInfos(){
        return tradeService.getAcctInfos();
    }



    public Boolean acctOperate(String acctId,Enums.MSG_TYPE type,String data){
        Message rsp=tradeService.request(acctId,type,data);
        return rsp.getSuccess();
    }

    private String getServerByAcct(String acctId){
        if(this.acctInfoMap.containsKey(acctId)){
            String serverId=this.acctInfoMap.get(acctId).getOwner();
            return serverId;
        }
        return null;
    }

    @EventListener(MessageEvent.class)
    public void eventHandler(MessageEvent messageEvent){
        this.webSocketService.push((Message) messageEvent.getSource());
    }
}
