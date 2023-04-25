/*!****************************************************************************
 @note   Copyright (coffee), 2005-2019, Shengli Tech. Co., Ltd.
 @file   efh_hpf_quote.h
 @date   2019/7/8   21:13
 @author zhou.hu

 @brief  本文件是EFH_HPF行情组播接口的示例程序， 定义了行情类型，结构，以及回调接口

 @note
******************************************************************************/



/**
 *     1、必须保证接收行情的网口是万兆网卡。
       2、网卡驱动必须是openonload-201811版本，更高版本是不支持的。但是向下兼容。

                idx（指数）组播地址信息如下：
               （1）行情组播地址 ：233.57.1.102
               （2）组播端口：37102
                接收组播数据的网段：192.168.99.X，本地端口可以任意指定（不与已有地址冲突即可）。

                exe（成交）行情组播地址信息如下：
               （3）行情组播地址 ：233.57.1.103
               （4）组播端口：37103
                接收组播数据的网段：192.168.99.X，本地端口可以任意指定（不与已有地址冲突即可）。

               lev2  组播地址信息如下：
               （5）行情组播地址：233.57.1.104
               （6）组播端口：37104
                接收组播数据的网段： 192.168.99.X，本地端口可以任意指定（不与已有地址冲突即可）。
 */

#pragma once
#include <vector>


using std::vector;


#define		SSE_MSG_TYPE_IDX			(33)
#define		SSE_MSG_TYPE_EXECUTION		(34)
#define		SSE_MES_TYPE_LEV2_FULL		(39)


#define		MAX_IP_LEN					(32)
#define		SYMBOL_LEN					(9)

typedef		unsigned int				T_PRICE_TYPE;
typedef		long long					T_QTY_TYPE;

typedef     unsigned int				T_PRICE_TYPE;
typedef     long long int				T_QUANTITY_TYPE;
typedef     long long int				T_VALUE_TYPE;
typedef     char						T_SYMBOL_TYPE[SYMBOL_LEN];


#ifdef USE_SF
    #include "socket_multicast_sf.hpp"
#else
    #include "socket_multicast.hpp"
#endif

using namespace multicast;



#pragma pack(push, 1)

struct sse_hpf_head
{
    unsigned int						m_sequence;
    unsigned short						m_tick;			/// 软件此值填0
    unsigned short						m_reserved;		/// 软件此值填0
    unsigned char						m_msg_type;
    unsigned short						m_msg_len;		/// 包括此消息头的长度，lev2_full=440， idx=80，timesale=88
    unsigned char						m_exchange_id;
    unsigned short						m_qt_year;
    unsigned char						m_qt_month;
    unsigned char						m_qt_day;
    unsigned int						m_send_time;
    unsigned char						m_category_id;
    unsigned int						m_msg_seq_id;
    unsigned char						m_seq_lost_flag;/// 1=有丢包，0=没有丢包
};

struct px_level
{
    px_level()
    {
        memset(this, 0, sizeof(px_level));
    }
    T_PRICE_TYPE						m_px;
    T_QTY_TYPE							m_qty;
};

struct sse_hpf_lev2
{
    sse_hpf_lev2()
    {
        memset(this, 0, sizeof(sse_hpf_lev2));
    }
    sse_hpf_head						m_head;

    unsigned int						m_update_time;
    T_SYMBOL_TYPE						m_symbol;
    unsigned char						m_sec_type;
    unsigned char						m_data_type;
    unsigned char						m_reserved1;
    T_PRICE_TYPE						m_pre_close_price;
    T_PRICE_TYPE 						m_open_px;
    T_PRICE_TYPE						m_day_high;
    T_PRICE_TYPE						m_day_low;
    T_PRICE_TYPE						m_last_px;
    T_PRICE_TYPE						m_close_px;
    unsigned char						m_instrument_status;
    unsigned char						m_trading_status;
    unsigned short						m_reserved2;
    unsigned int						m_total_trade_number;
    T_QUANTITY_TYPE						m_total_qty;
    T_VALUE_TYPE						m_total_value;

    T_QUANTITY_TYPE						m_total_bid_qty;
    T_PRICE_TYPE						m_total_bid_weighted_avg_px;
    T_QUANTITY_TYPE						m_total_ask_qty;
    T_PRICE_TYPE						m_total_ask_weighted_avg_Px;
    unsigned int						m_yield_to_maturity;
    unsigned char						m_bid_depth;
    unsigned char						m_ask_depth;

    px_level							m_bid_px[10];
    px_level							m_ask_px[10];
};


struct sse_hpf_idx
{
    sse_hpf_idx()
    {
        memset(this, 0, sizeof(sse_hpf_idx));
    }
    sse_hpf_head						m_head;

    unsigned int						m_update_time;
    T_SYMBOL_TYPE						m_symbol;
    unsigned char						m_sec_type;			/// 0=指数，1=股票，3=债券&期权 10=其他
    T_PRICE_TYPE						m_pre_close_px;
    T_PRICE_TYPE 						m_open_px;
    T_VALUE_TYPE						m_total_value;
    T_PRICE_TYPE						m_high;
    T_PRICE_TYPE						m_low;
    T_PRICE_TYPE						m_last_px;
    T_QUANTITY_TYPE						m_total_qty;
    T_PRICE_TYPE						m_close_px;
};

struct sse_hpf_exe
{
    sse_hpf_exe()
    {
        memset(this, 0, sizeof(sse_hpf_exe));
    }
    sse_hpf_head						m_head;

    unsigned int						m_exe_sequence_num;
    unsigned int						m_channel_num;
    T_SYMBOL_TYPE						m_symbol;
    unsigned int						m_trade_time;
    T_PRICE_TYPE						m_exe_px;
    T_QUANTITY_TYPE						m_exe_qty;
    T_VALUE_TYPE						m_exe_value;
    unsigned long long int				m_exe_buy_num;
    unsigned long long int				m_exe_sell_num;
    char								m_exe_side_flag;
    unsigned int						m_reserved;
};

struct multicast_info
{
    char	m_remote_ip[MAX_IP_LEN];		///< 组播行情远端地址
    int		m_remote_port;					///< 组播行情远端端口
#ifdef USE_SF
    char	m_local_eth[MAX_IP_LEN];		///< 组播本机网卡
#else
    char	m_local_ip[MAX_IP_LEN];			///< 组播本机地址
    int		m_local_port;					///< 组播本机端口
#endif

};

#pragma pack(pop)


class efh_hpf_quote_event
{
public:
    virtual ~efh_hpf_quote_event() {}
    /// \brief 接收到组播数据的回调事件
    virtual void on_receive_lev2(sse_hpf_lev2* data) = 0;
    virtual void on_receive_idx(sse_hpf_idx* data) = 0;
    virtual void on_receive_exe(sse_hpf_exe* data) = 0;
};

class efh_hpf_quote : public socket_event
{
public:
    efh_hpf_quote(void){
        m_ptr_event = NULL;
    }
    ~efh_hpf_quote(void){

    }

    /// \brief 初始化
    bool init(multicast_info conf, efh_hpf_quote_event* p_event){
        m_conf = conf;
        m_ptr_event = p_event;

#ifdef USE_SF
        bool ret = m_udp.sock_init(m_conf.m_remote_ip, m_conf.m_remote_port, m_conf.m_local_eth, 0, this);
#else
        bool ret = m_udp.sock_init(m_conf.m_remote_ip, m_conf.m_remote_port, m_conf.m_local_ip, m_conf.m_local_port, 0, this);
#endif
        if (!ret)
        {
            return false;
        }

        return true;
    }

    /// \brief 关闭
    void close(){
        m_udp.sock_close();
    }

private:
    /// \brief 组播数据接收回调接口
    virtual void on_receive_message(int id, const char* buff, unsigned int len){
        if (!m_ptr_event)
        {
            return;
        }

        sse_hpf_head* ptr_head = (sse_hpf_head*)buff;
        switch(ptr_head->m_msg_type)
        {
            case SSE_MSG_TYPE_IDX:
            {
                sse_hpf_idx* ptr_data = (sse_hpf_idx*)(buff);
                m_ptr_event->on_receive_idx(ptr_data);
            }
                break;
            case SSE_MSG_TYPE_EXECUTION:
            {
                sse_hpf_exe* ptr_data = (sse_hpf_exe*)(buff);
                m_ptr_event->on_receive_exe(ptr_data);
            }
                break;
            case SSE_MES_TYPE_LEV2_FULL:
            {
                sse_hpf_lev2* ptr_data = (sse_hpf_lev2*)(buff);
                m_ptr_event->on_receive_lev2(ptr_data);
            }
            default:
            {

            }
                break;
        }
    }

private:
    socket_multicast		m_udp;				///< UDP行情接收接口
    multicast_info			m_conf;				///< 配置接口信息
    efh_hpf_quote_event*	m_ptr_event;		///< 行情回调事件接口
};

