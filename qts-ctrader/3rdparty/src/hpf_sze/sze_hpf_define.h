/*!****************************************************************************
	@file   	sze_hpf_define.h
	
	@brief		盛立行情输出结构体
******************************************************************************/
#pragma once

#define		SZE_EXCHANGE_ID				(101)

#define		SZE_REPLACE_MSG_TYPE		(21)
#define		SZE_IDX_MSG_TYPE			(22)
#define		SZE_ORDER_MSG_TYPE			(23)
#define		SZE_EXECUTION_MSG_TYPE		(24)
#define		SZE_TREE_MSG_TYPE			(29)


#pragma pack(push, 1)
struct px_qty_unit
{
	unsigned int						m_price;
	long long							m_qty;
};

struct sze_hpf_pkt_head
{
	int									m_sequence;
	short								m_tick1;
	short								m_tick2;
	unsigned char						m_msg_type;
	unsigned char						m_security_type;
	unsigned char						m_sub_security_type;
	char								m_symbol[9];
	unsigned char						m_exchange_id;
	long long							m_quote_update_time;
	unsigned short						m_channel_no;
	long long							m_sequence_num;
	unsigned int 						m_md_stream_id;
};

struct sze_hpf_lev2_pkt
{
	sze_hpf_pkt_head					m_header;
	unsigned char 						m_trade_phase_code;
	long long							m_trade_num;
	long long							m_total_qty;
	long long							m_total_value;
	unsigned int						m_pre_close_price;
	unsigned int						m_last_price;
	unsigned int						m_open_price;
	unsigned int						m_day_high;
	unsigned int						m_day_low;
	unsigned int						m_today_close_price;
	unsigned int						m_total_bid_wvp;
	long long							m_total_bid_qty;
	unsigned int						m_total_ask_wvp;
	long long							m_total_ask_qty;
	unsigned int 						m_lpv;
	unsigned int 						m_iopv;
	unsigned int						m_upper_limit_price;
	unsigned int						m_low_limit_price;
	unsigned int 						m_open_interest;
	px_qty_unit							m_bid_unit[10];
	px_qty_unit							m_ask_unit[10];
};

struct sze_hpf_idx_pkt
{
	sze_hpf_pkt_head					m_header;
	long long							m_trade_num;
	long long							m_total_qty;
	long long							m_total_value;
	unsigned int						m_last_price;
	unsigned int						m_pre_close_price;
	unsigned int						m_open_price;
	unsigned int						m_day_high;
	unsigned int						m_day_low;
	unsigned int 						m_close_price;
	char								m_resv[5];
};

struct sze_hpf_order_pkt
{
	sze_hpf_pkt_head					m_header;
	unsigned int						m_px;
	long long							m_qty;
	char								m_side;
	char								m_order_type;
	char								m_resv[7];
};

struct sze_hpf_exe_pkt
{
	sze_hpf_pkt_head					m_header;
	long long							m_bid_app_seq_num;
	long long							m_ask_app_seq_num;
	unsigned int						m_exe_px;
	long long							m_exe_qty;
	char								m_exe_type;
};

struct sze_hpf_tree_pkt
{
	sze_hpf_pkt_head					m_header;
	long long							m_trade_num;
	unsigned long long					m_total_qty;
	long long							m_total_value;
	unsigned int						m_pre_close_price;
	unsigned int						m_last_price;
	unsigned int						m_open_price;
	unsigned int						m_day_high;
	unsigned int						m_day_low;
	unsigned int						m_today_close_price;
	unsigned int						m_total_bid_wvp;
	unsigned long long					m_total_bid_qty;
	unsigned int						m_total_ask_wvp;
	unsigned long long					m_total_ask_qty;
	unsigned int						m_upper_limit_price;
	unsigned int						m_low_limit_price;
	unsigned long long					m_market_open_total_bid;
	unsigned long long					m_market_open_total_ask;
	unsigned int						m_total_lev2_bid;
	unsigned int						m_total_lev2_ask;
	px_qty_unit							m_bid_unit[10];
	px_qty_unit							m_ask_unit[10];
	char								m_resv[5];
};

#pragma pack(pop)


