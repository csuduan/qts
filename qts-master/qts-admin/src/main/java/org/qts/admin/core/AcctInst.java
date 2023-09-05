package org.qts.admin.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.qts.admin.entity.AcctDesc;
import org.qts.admin.exception.BizException;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.event.MessageEvent;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Position;
import org.qts.common.entity.trade.Tick;
import org.qts.common.entity.trade.Trade;
import org.qts.common.rpc.tcp.client.MsgHandler;
import org.qts.common.rpc.tcp.client.TcpClient;
import org.qts.common.utils.ProcessUtil;
import org.qts.common.utils.SequenceUtil;
import org.qts.common.utils.SpringUtils;
import org.springframework.beans.BeanUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class AcctInst  implements MsgHandler {
    private AcctDetail acct;
    private String id;
    private Enums.ACCT_STATUS status;
    private Integer pid = 0;
    private String updateTimes;

    private TcpClient tcpClient;

    public AcctInst(AcctConf conf){
        this.id = conf.getId();
        this.acct = new AcctDetail(conf);
        this.status = Enums.ACCT_STATUS.UNSTARTED;
        this.updateTimes = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        this.tcpClient = new TcpClient(this.acct.getId(),this.acct.getConf().getPort(),this);
    }
    public AcctDesc getAcctInstDesc(){
        AcctDesc acctDesc =new AcctDesc();
        BeanUtils.copyProperties(this.acct,acctDesc);
        acctDesc.setStatus(this.status);
        acctDesc.setUpdateTimes(this.updateTimes);
        return acctDesc;
    }

    public void chekcInstStatus(){
        //检查账户进程是否启动
        int pid= ProcessUtil.getProcess("qts-trader","acctId="+this.getId());
        if(pid<=0){
            if(this.pid>0)
                log.info("账户[{}]已停止,pid:{}",this.getId(),this.pid);
            this.setStatus(Enums.ACCT_STATUS.UNSTARTED);
            this.getTcpClient().stop();
            this.pid=0;
            return;
        }
        if(this.pid==0)
            log.info("账户[{}]已启动,pid:{}",this.getId(),pid);
        this.pid = pid;
        this.setStatus(Enums.ACCT_STATUS.CONNING);
        //启动客户端
        this.tcpClient.start();
        if(this.tcpClient.isConnected())
            this.setStatus(Enums.ACCT_STATUS.READY);
       this.updateTimes = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

       //广播状态
        SpringUtils.pushEvent(new MessageEvent(new Message(Enums.MSG_TYPE.ON_ACCT,this.getAcctInstDesc())));

    }

    public void startProcess(){
        try {
            List<String> cmds = new ArrayList<String>();
            cmds.add("java");
            cmds.add("-DacctId="+this.id);
            cmds.add("-jar");
            //cmds.add("-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
            cmds.add("qts-trader/target/qts-trader.jar");
            pid=ProcessUtil.startProcess(cmds)  ;
            this.pid=pid;
        }catch (Exception ex){
            log.error("启动账户[{}]失败",this.id,ex);
        }
    }

    public Message request(Message req){
        String requestId = SequenceUtil.getLocalSerialNo(16);
        req.setRequestId(requestId);
        log.info("request==>req:{}",req);
        if(this.status!= Enums.ACCT_STATUS.READY && !tcpClient.isConnected()){
            throw  new BizException("账户未就绪");
        }
        Message rsp= tcpClient.request(req);
        log.info("request==>rsp:{}",rsp);
        return rsp;
    }

    @Override
    public void onMessage(Message msg) {
        switch (msg.getType()){
            case ON_DETAIL -> {
                AcctDetail acctDetail = msg.getData(AcctDetail.class);
                if(acctDetail!=null){
                    this.acct =acctDetail;
                    log.info("更新账户明细,{}",this.acct.getId());
                }

            }
            case ON_ACCT -> {
                AcctInfo acctInfo = msg.getData(AcctInfo.class);
                BeanUtils.copyProperties(this.acct,acctInfo);
                log.info("更新账户信息,{}",this.acct.getId());
            }
            case ON_POSITION -> {
                Position pos = msg.getData(Position.class);
                this.acct.getPositions().put(pos.getId(),pos);
            }
            case ON_TRADE -> {
                Trade trade = msg.getData(Trade.class);
                this.acct.getTradeList().put(trade.getTradeID(),trade);
            }
            case ON_ORDER -> {
                Order order = msg.getData(Order.class);
                this.acct.getOrders().put(order.getOrderSysID(),order);
                if(order.isFinished())
                    this.acct.getOrders().remove(order.getOrderSysID());
            }
            case ON_TICK -> {
                Tick tick = msg.getData(Tick.class);
                this.acct.getTicks().put(tick.getSymbol(),tick);
            }
        }
    }
}
