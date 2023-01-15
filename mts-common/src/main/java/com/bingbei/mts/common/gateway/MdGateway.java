package com.bingbei.mts.common.gateway;

import com.bingbei.mts.common.entity.LoginInfo;
import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.entity.SubscribeReq;
import com.bingbei.mts.common.entity.Tick;

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
     * @param subscribeReq
     */
    void subscribe(SubscribeReq subscribeReq);

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
    public void emitTick(Tick tick);
}
