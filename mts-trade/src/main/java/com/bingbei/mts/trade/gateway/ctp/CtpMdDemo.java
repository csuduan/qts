package com.bingbei.mts.trade.gateway.ctp;

import xyz.redtorch.gateway.ctp.x64v6v3v19p1v.api.*;

public class CtpMdDemo extends CThostFtdcMdSpi {
    static{
        System.out.println(	System.getenv().get("LD_LIBRARY_PATH"));
        System.out.println(System.getProperty("java.library.path"));
        System.loadLibrary("thostmduserapi_se");
        System.loadLibrary("jctpv6v3v19p1x64api");
    }
    final static String ctp1_mdAddress = "tcp://180.168.146.187:10211";
    //final static String ctp1_TradeAddress = "tcp://172.24.125.199:50233";
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        CThostFtdcMdApi traderApi = CThostFtdcMdApi.CreateFtdcMdApi();
        CtpMdDemo pTraderSpi = new CtpMdDemo();
        traderApi.RegisterSpi(pTraderSpi);
        traderApi.RegisterFront(ctp1_mdAddress);
        traderApi.Init();
        traderApi.Join();
        return;
    }

    public CtpMdDemo(){
    }



}
