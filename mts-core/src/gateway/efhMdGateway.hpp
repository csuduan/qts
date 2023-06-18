//
// Created by 段晴 on 2022/2/22.
//

#ifndef MTS_CORE_ELFMDGATEWAY_H
#define MTS_CORE_ELFMDGATEWAY_H

#include <stdlib.h>

#include "gateway.h"
#include "common/sse_hpf_quote.hpp"
#include "define.h"
#include "acct.h"

class ElfMdGateway :public efh_hpf_quote_event,public MdGateway{
public:
    ElfMdGateway(Acct* acct): acct(acct){
        this->queue=acct->mdQueue;

    }
    ~ElfMdGateway(){}
    virtual void on_receive_lev2(sse_hpf_lev2* ptr){
        char buff[8192];

        memset(buff, 0, sizeof(buff));
        sprintf(buff, "%u,%u,%u,%s,%u,%u,%u,%u,%u,%u,%u,%u,%lld,%u,%lld"
                ,ptr->m_head.m_sequence
                ,ptr->m_head.m_category_id
                ,ptr->m_head.m_msg_seq_id
                ,ptr->m_symbol
                ,ptr->m_update_time

                ,ptr->m_open_px
                ,ptr->m_day_high
                ,ptr->m_day_low
                ,ptr->m_close_px
                ,ptr->m_total_bid_weighted_avg_px
                ,ptr->m_total_ask_weighted_avg_Px
                ,ptr->m_bid_px[0].m_px
                ,ptr->m_bid_px[0].m_qty
                ,ptr->m_ask_px[0].m_px
                ,ptr->m_ask_px[0].m_qty
        );

        string str = buff;
        cout <<  "lev2: " << str << endl;
    }
    virtual void on_receive_idx(sse_hpf_idx* ptr){
        char buff[8192];

        memset(buff, 0, sizeof(buff));
        sprintf(buff, "%u,%u,%u,%s,%u,%u,%u,%u,%u"
                ,ptr->m_head.m_sequence
                ,ptr->m_head.m_category_id
                ,ptr->m_head.m_msg_seq_id
                ,ptr->m_symbol
                ,ptr->m_update_time

                ,ptr->m_open_px
                ,ptr->m_high
                ,ptr->m_low
                ,ptr->m_last_px
        );

        string str = buff;

        cout <<  "idx: " << str << endl;
    }
    virtual void on_receive_exe(sse_hpf_exe* ptr){
        char buff[8192];

        memset(buff, 0, sizeof(buff));
        sprintf(buff, "%u,%u,%u,%s,%u,%u,%lld"
                ,ptr->m_head.m_sequence
                ,ptr->m_head.m_category_id
                ,ptr->m_head.m_msg_seq_id
                ,ptr->m_symbol
                ,ptr->m_trade_time

                ,ptr->m_exe_px
                ,ptr->m_exe_qty
        );

        string str = buff;

        cout <<  "exe: " << str << endl;
    }

    void subscribe(set<string> &contracts){}
    void reSubscribe(){}
    int  connect(){
        vector<string> mdList;
        Util::split(this->acct->acctConf->mdAddress,mdList,",");
        for(auto & md : mdList){
            multicast_info multicastInfo={0};
            //mdAddress格式 组播地址:端口:本地网卡
            vector<string> tmp;
            Util::split(md,tmp,":");
            if(tmp.size()!=4){
                loge("无效的ELF组播地址 {}",md);
                return -1;
            }
            string type=tmp[0];
            strcpy(multicastInfo.m_remote_ip,tmp[1].c_str());
            multicastInfo.m_remote_port=atoi(tmp[2].c_str());
            strcpy(multicastInfo.m_local_eth,tmp[3].c_str());
            this->m_efh_hpfMap[type].init(multicastInfo,this);
        }

        this->isConnected=true;
    }
    void disconnect(){
        m_efh_hpf.close();
        this->isConnected= false;

    }

private:
    Acct* acct;
    LockFreeQueue<Event> *queue;

    efh_hpf_quote	m_efh_hpf;			///< 行情接收对象
    bool  isConnected;
    string tradingDay;
    std::set<string> contracts;
    vector<Quote *> quotes;

    map<string,efh_hpf_quote> m_efh_hpfMap;
    void Run();

};


#endif //MTS_CORE_ELFMDGATEWAY_H
