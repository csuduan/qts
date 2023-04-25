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
    multicast_info multicastInfo={0};
    //mdAddress格式 组播地址:端口:本地网卡
    vector<string> tmp;
    Util::split(this->quote->address,tmp,":");
    if(tmp.size()!=3){
        loge("无效的ELF组播地址 {}",this->quote->address);
        return -1;
    }
    strcpy(multicastInfo.m_remote_ip,tmp[0].c_str());
    multicastInfo.m_remote_port=atoi(tmp[1].c_str());
    strcpy(multicastInfo.m_local_eth,tmp[2].c_str());

    m_efh_hpf.init(multicastInfo,this);


}

void ElfMdGateway::disconnect() {
    m_efh_hpf.close();
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