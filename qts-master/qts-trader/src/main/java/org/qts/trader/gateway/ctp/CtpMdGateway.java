package org.qts.trader.gateway.ctp;


import org.qts.common.disruptor.FastEventService;
import org.qts.common.entity.MdInfo;
import org.qts.common.gateway.AbsMdGateway;

import java.util.HashSet;
import java.util.List;

public class CtpMdGateway extends AbsMdGateway {
    private MdSpi mdSpi =null;
    private HashSet<String> subscribedSymbols = new HashSet<>();

    public CtpMdGateway(FastEventService fastEventEngineService, MdInfo mdInfo) {
        super(fastEventEngineService, mdInfo);
    }

    @Override
    public void connect() {
        if(this.mdSpi!=null)
            this.mdSpi.close();
        //重新实例化接口
        this.mdSpi= new MdSpi(this);
        this.mdSpi.connect();
        //this.subscribe("cu2202");
    }

    @Override
    public void close() {
        //仅释放接口
        if(this.mdSpi!=null)
            this.mdSpi.close();
        this.mdSpi=null;
    }




    @Override
    public void subscribe(String symbol) {
        this.subscribedSymbols.add(symbol);
        if(this.mdSpi!=null && this.mdSpi.isConnected())
            this.mdSpi.subscribe(symbol);
    }

    @Override
    public void unSubscribe(String stdSymbol) {
        this.subscribedSymbols.remove(stdSymbol);
        if(this.mdSpi!=null && this.mdSpi.isConnected())
            this.mdSpi.unSubscribe(stdSymbol);
    }

    public void onConnect(){
        this.subscribedSymbols.forEach(x->{
            this.mdSpi.unSubscribe(x);
        });

    }

    @Override
    public boolean isConnected() {
        return this.mdSpi!=null && this.mdSpi.isConnected();
    }

    public void onClose(){
        this.mdSpi=null;
    }


    @Override
    public List<String> getSubscribedSymbols() {
        return this.subscribedSymbols.stream().toList();
    }
}
