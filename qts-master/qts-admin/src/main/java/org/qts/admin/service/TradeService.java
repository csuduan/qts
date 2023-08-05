package org.qts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.qts.admin.dao.ConfMapper;
import org.qts.admin.exception.BizException;
import org.fts.common.entity.Message;
import org.fts.common.entity.acct.AcctInst;
import org.fts.common.entity.acct.AcctInfo;
import org.fts.common.entity.event.MessageEvent;
import org.fts.common.entity.msg.ConfMsg;
import org.fts.common.rpc.listener.ClientListener;
import org.fts.common.rpc.uds.UdsClient;
import org.fts.common.utils.SequenceUtil;
import org.fts.common.utils.SpringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TradeService implements ClientListener {

    @Autowired
    private CacheService cacheService;
    @Autowired
    private ConfMapper confMapper;

    @Value("${server.name}")
    private String owner;

    private Map<String, AcctInst> acctInstMap=new HashMap<>();

    @PostConstruct
    public void init(){
    }
    public List<AcctInfo> getAcctInfos(){
        List<AcctInfo> result=new ArrayList<>();
        acctInstMap.forEach((x,y)->result.add(y.getAcctInfo()));
        return result;
    }

    public boolean updateConf(ConfMsg confMsg){
        //为了不引起已就绪的交易核心因账户配置刷新引起的问题，不主动推新的配置给交易核心
        if(!CollectionUtils.isEmpty(confMsg.getAcctConfList())){
            for(var acctConf :confMsg.getAcctConfList()){
                //this.cacheService.save(ACCT,acctConf.getId(),acctConf);
                //this.acctConfMap.put(acctConf.getId(),acctConf);
                //todo 更新CONF表
                //this.startAcctClient(acctConf);
            }
        }
        return true;
    }


    /**
     * 启动acctClient
     * @param acctConf
     * @return
     */
    public boolean startAcctClient(AcctConf acctConf){
        if(acctConf.getEnable()==false)
            return false;
        if(this.acctInstMap.containsKey(acctConf.getId()))
            return false;

        AcctInst acctInst=new AcctInst(acctConf);
        this.acctInstMap.put(acctInst.getAcctId(),acctInst);

        UdsClient udsClient=new UdsClient(acctConf.getId(),this);
        acctInst.setUdsClient(udsClient);
        udsClient.start();

        return true;
    }

    /**
     * 关闭acctClient
     * @param acctId
     * @return
     */
    private boolean stopAcctClient(String acctId){
        if(this.acctInstMap.containsKey(acctId)){
            this.acctInstMap.get(acctId).getUdsClient().close();
            this.acctInstMap.remove(acctId);
        }
        return true;
    }

    public org.fts.common.entity.Message.Message request(String acctId, Enums.MSG_TYPE type, Object data){
        return this.request(acctId,new org.fts.common.entity.Message.Message(type,data));
    }

    public org.fts.common.entity.Message.Message request(String acctId, org.fts.common.entity.Message.Message req){
        if(!this.acctInstMap.containsKey(acctId)
                || !this.acctInstMap.get(acctId).getUdsClient().isConnected())
            throw  new BizException("账户未连接");

        String requestId = SequenceUtil.getLocalSerialNo(16);
        req.setRequestId(requestId);
        log.info("request==>req:{}",req);
        var client=this.acctInstMap.get(acctId).getUdsClient();
        org.fts.common.entity.Message.Message rsp=client.request(req);
        log.info("request==>rsp:{}",rsp);
        return  rsp;
    }


    @Override
    public void onStatus(String id,boolean status) {
        log.info("acct client[{}] status changed:{}",id,status);
        AcctInst acctInst=this.acctInstMap.get(id);
        if(status==true){
            acctInst.getAcctInfo().setStatus(true);
            //查询账户信息
            try {
                org.fts.common.entity.Message.Message rsp=this.request(id,Enums.MSG_TYPE.QRY_ACCT,null);
                AcctInfo acctInfo=rsp.getData(AcctInfo.class);
                BeanUtils.copyProperties(acctInfo,acctInst.getAcctInfo());
                acctInst.getAcctInfo().setStatus(true);
                acctInst.getAcctInfo().setStatusMsg("已就绪");

            }catch (Exception ex){
                log.error("qry acct error!",ex);
            }

        }else{
            acctInst.getAcctInfo().setStatus(false);
            acctInst.getAcctInfo().setStatusMsg("未连接");
            acctInst.getAcctInfo().setTdStatus(false);
            acctInst.getAcctInfo().setMdStatus(false);
        }
        //推送给admin
        SpringUtils.pushEvent(new MessageEvent(new org.fts.common.entity.Message.Message(Enums.MSG_TYPE.ON_ACCT,acctInst.getAcctInfo())));
    }

    @Override
    public void onMessage(org.fts.common.entity.Message.Message msg) {
        log.info("onMessage:{}",msg);
        AcctInst acctInst=this.acctInstMap.get(msg.getAcctId());
        switch (msg.getType()){
            case ON_ACCT -> {
                //Message rsp=this.request(acctInst.getAcctId(),new Message(Enums.MSG_TYPE.QRY_ACCT,null));
                AcctInfo acctInfo=msg.getData(AcctInfo.class);
                //BeanUtils.copyProperties(acctInfo,acctInst.getAcctInfo());
                acctInst.getAcctInfo().setTdStatus(acctInfo.getTdStatus());
                acctInst.getAcctInfo().setMdStatus(acctInfo.getMdStatus());
                SpringUtils.pushEvent(new MessageEvent(new Message.Message(Enums.MSG_TYPE.ON_ACCT,acctInst.getAcctInfo())));
            }
        }
        //再次推送
        //SpringUtils.pushEvent(new MessageEvent(msg));
    }
}
