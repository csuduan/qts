package com.bingbei.mts.adapter.ctp;

import com.bingbei.mts.common.entity.Account;
import com.bingbei.mts.common.entity.LoginInfo;
import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.entity.SubscribeReq;
import com.bingbei.mts.common.gateway.MdGatewayAbstract;
import com.bingbei.mts.common.service.FastEventEngineService;

import java.util.HashSet;
import java.util.List;

public class CtpMdGateway extends MdGatewayAbstract {
    private MdSpi mdSpi =null;
    private HashSet<String> subscribedSymbols = new HashSet<>();

    public CtpMdGateway(FastEventEngineService fastEventEngineService, MdInfo mdInfo) {
        super(fastEventEngineService, mdInfo);
    }

    @Override
    public void connect() {
        if(this.mdSpi!=null)
            this.mdSpi.close();
        //重新实例化接口
        this.mdSpi= new MdSpi(this);
    }

    @Override
    public void close() {
        //仅释放接口
        if(this.mdSpi!=null)
            this.mdSpi.close();
    }




    @Override
    public void subscribe(SubscribeReq subscribeReq) {
        this.subscribedSymbols.add(subscribeReq.getSymbol());
        if(this.mdSpi.isConnected())
            this.mdSpi.subscribe(subscribeReq.getSymbol());
    }

    @Override
    public void unSubscribe(String stdSymbol) {
        this.subscribedSymbols.remove(stdSymbol);
        if(this.mdSpi.isConnected())
            this.mdSpi.unSubscribe(stdSymbol);
    }


    @Override
    public List<String> getSubscribedSymbols() {
        return this.subscribedSymbols.stream().toList();
    }
}
