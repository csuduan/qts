//
// Created by 段晴 on 2022/2/22.
//

#ifndef MTS_CORE_ELFMDGATEWAY_H
#define MTS_CORE_ELFMDGATEWAY_H

#include "Gateway.h"
#include "sse_hpf_quote.hpp"
#include "define.h"

class ElfMdGateway :public efh_hpf_quote_event,public MdGateway{
public:
    ElfMdGateway(Quote * quote):MdGateway(quote){
    }
    ~ElfMdGateway(){}
    virtual void on_receive_lev2(sse_hpf_lev2* data);
    virtual void on_receive_idx(sse_hpf_idx* data);
    virtual void on_receive_exe(sse_hpf_exe* data);

    void subscribe(set<string> &contracts);
    void reSubscribe();
    int  connect();
    void disconnect();

private:
    efh_hpf_quote	m_efh_hpf;			///< 行情接收对象
    bool  isConnected;
    string tradingDay;
    std::set<string> contracts;
    LockFreeQueue<Event>* queue;
    map<string, Order *> orderMap;
    void Run();

};


#endif //MTS_CORE_ELFMDGATEWAY_H
