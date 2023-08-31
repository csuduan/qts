package org.qts.trader.gateway;


import java.util.List;

public interface MdGateway {

    /**
     * 订阅
     *
     * @param symbols
     */
    void subscribe(List<String> symbols);


    /**
     * 连接
     */
    void connect();

    /**
     * 关闭
     */
    void close();


    boolean isConnected();
}
