//
// Created by 段晴 on 2022/2/22.
//

#include "EfhMdGateway.h"
#include <stdlib.h>
#include <unistd.h>

void ElfMdGateway::subscribe(set<string> &contracts) {

}

void ElfMdGateway::reSubscribe() {

}

int ElfMdGateway::connect() {
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

void ElfMdGateway::disconnect() {
    m_efh_hpf.close();
    this->isConnected= false;

}


void ElfMdGateway::on_receive_lev2(sse_hpf_lev2* ptr)
{
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

void ElfMdGateway::on_receive_idx(sse_hpf_idx* ptr)
{
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


void ElfMdGateway::on_receive_exe(sse_hpf_exe* ptr)
{
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