package org.qts.trader.gateway;


import org.qts.common.entity.MdInfo;
import org.qts.common.entity.trade.Tick;

import java.util.List;

public interface MdGateway {

    /**
     * 订阅
     *
     * @param symbol
     */
    void subscribe(String symbol);


    /**
     * 连接
     */
    void connect();

    /**
     * 关闭
     */
    void close();


    List<String> getSubscribedSymbols();



    boolean isConnected();
}
