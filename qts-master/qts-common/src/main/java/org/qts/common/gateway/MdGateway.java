package org.qts.common.gateway;


import org.qts.common.entity.MdInfo;
import org.qts.common.entity.trade.Tick;

import java.util.List;

public interface MdGateway {

    /**
     * 获取行情信息
     * @return
     */
    MdInfo getMdInfo();
    /**
     * 订阅
     *
     * @param symbol
     */
    void subscribe(String symbol);

    /**
     * 退订
     *
     * @param stdSymbol
     */
    void unSubscribe(String stdSymbol);

    /**
     * 连接
     */
    void connect();

    /**
     * 关闭
     */
    void close();

    List<String> getSubscribedSymbols();


    /**
     * 发送Tick事件
     *
     * @param tick
     */
    void emitTick(Tick tick);


    /**
     * 响应连接事件
     */
    void onConnect();

    boolean isConnected();
}
