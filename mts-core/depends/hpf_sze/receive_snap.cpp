#include <iostream>
#include <sstream>
#include <algorithm>
#include <cmath>
#include <vector>
#include <time.h>
#include "receive_snap.h"
#include "sze_hpf_define.h"

using namespace std;

udp_quote_snap::udp_quote_snap(void)
{
}

udp_quote_snap::~udp_quote_snap(void)
{
}

//***************************************************************************
//*********************重点接收处理level2行情部分代码************************
//***************************************************************************
void udp_quote_snap::on_receive_message(int id, const char* buff, unsigned int len)
{
	/// 数据处理部分
	const char*	ptr_udp		= buff;
	int	remain_len			= len;
	for (; remain_len > 0;)
	{
		sze_hpf_lev2_pkt* p_snp = (sze_hpf_lev2_pkt*)ptr_udp;

		timespec					ts;
		clock_gettime(CLOCK_REALTIME, &ts);

		int node = m_ll_snap_count % QT_SNAP_COUNT;
		memcpy(m_snap[node].m_symbol, p_snp->m_header.m_symbol, 9);
		//m_sl_snp_que[node].m_channel_no = p_snp->m_header.m_channel_no;
		m_snap[node].m_quote_update_time = p_snp->m_header.m_quote_update_time;
		m_snap[node].m_local_time = ((long long)(ts.tv_sec)) * 1000000000 + ts.tv_nsec;

		for (int i = 1; i < 9; ++i)
		{
			p_snp->m_bid_unit[i].m_price = p_snp->m_bid_unit[i].m_price / 1000000;
			p_snp->m_bid_unit[i].m_qty = p_snp->m_bid_unit[i].m_qty / 100;
			p_snp->m_ask_unit[i].m_price = p_snp->m_ask_unit[i].m_price / 1000000;
			p_snp->m_ask_unit[i].m_qty = p_snp->m_ask_unit[i].m_qty / 100;
		}

		p_snp->m_trade_num = p_snp->m_trade_num / 10000;
		p_snp->m_total_value = p_snp->m_total_value / 1000000;
		p_snp->m_pre_close_price = p_snp->m_pre_close_price / 1000000;
		p_snp->m_today_close_price = p_snp->m_today_close_price / 1000000;
		p_snp->m_upper_limit_price = p_snp->m_upper_limit_price / 1000000;
		p_snp->m_low_limit_price = p_snp->m_low_limit_price / 1000000;
		p_snp->m_total_bid_wvp = p_snp->m_total_bid_wvp / 1000000;
		p_snp->m_total_ask_wvp = p_snp->m_total_ask_wvp / 1000000;

		m_snap[node].m_seq				= p_snp->m_header.m_sequence;
		m_snap[node].m_bid_lev1.m_price = (double)(p_snp->m_bid_unit[0].m_price) / 1000000;
		m_snap[node].m_bid_lev1.m_qty = (p_snp->m_bid_unit[0].m_qty / 100);
		m_snap[node].m_bid_lev10.m_price = (double)(p_snp->m_bid_unit[9].m_price) / 1000000;
		m_snap[node].m_bid_lev10.m_qty = (p_snp->m_bid_unit[9].m_qty / 100);
		m_snap[node].m_ask_lev1.m_price = (double)(p_snp->m_ask_unit[0].m_price) / 1000000;
		m_snap[node].m_ask_lev1.m_qty = (p_snp->m_ask_unit[0].m_qty / 100);
		m_snap[node].m_ask_lev10.m_price = (double)(p_snp->m_ask_unit[9].m_price) / 1000000;
		m_snap[node].m_ask_lev10.m_qty = (p_snp->m_ask_unit[9].m_qty / 100);

		m_snap[node].m_open_price = (double)(p_snp->m_open_price) / 1000000;
		m_snap[node].m_day_high = (double)(p_snp->m_day_high) / 1000000;
		m_snap[node].m_day_low = (double)(p_snp->m_day_low) / 1000000;
		m_snap[node].m_last_price = (double)(p_snp->m_last_price) / 1000000;
		m_snap[node].m_total_qty = (p_snp->m_total_qty / 100);
		m_snap[node].m_total_bid_qty = (p_snp->m_total_bid_qty / 100);
		m_snap[node].m_total_ask_qty = (p_snp->m_total_ask_qty / 100);

		++m_ll_snap_count;
		remain_len -= sizeof(sze_hpf_lev2_pkt);
		ptr_udp += sizeof(sze_hpf_lev2_pkt);

	}
	/// 数据处理完成
	/***************网络处理完成**********************/
}

bool udp_quote_snap::init(const char* remote_ip, unsigned short remote_port,const char* local_ip, unsigned short local_port)
{
	bool ret = m_sock.sock_init( remote_ip, remote_port, local_ip, local_port, 0, static_cast<socket_event*>(this) );
	if( !ret )
	{
		return false;
	}

	/// 初始化内存
	for (int i = 0; i < QT_SNAP_COUNT; i++)
	{
		memset(&m_snap[i], 0, sizeof(qt_node_snap));
	}

	return ret;
}

void udp_quote_snap::close()
{
	m_sock.sock_close();
	sleep(10);
	log_quote();
}

void udp_quote_snap::write_log(const char* msg)
{
	cout << msg << endl;
}


void udp_quote_snap::log_quote()
{
	time_t now		= time(NULL);
	tm* ltm			= localtime(&now);
	
	char	str_full_name[1024];
	memset( str_full_name, 0, sizeof(str_full_name) );
	sprintf( str_full_name, "%04d%02d%02d_snap.csv", ltm->tm_year + 1900, ltm->tm_mon + 1, ltm->tm_mday );


	FILE* fp		= fopen(str_full_name, "at+");
	if( fp == NULL )
	{
		return;
	}

	if (m_ll_snap_count > 0)
	{
		for (int i = 0; i < QT_SNAP_COUNT; i++)
		{
			if (m_snap[i].m_local_time == 0)
			{
				break;
			}

			char ch_buffer[1024];
			memset(ch_buffer, 0, sizeof(ch_buffer));
			sprintf(ch_buffer, "%s, %lld, %lld, %.2f, %lld, %.2f, %lld, %.2f, %lld, %.2f, %lld, %lld, %lld, %.2f, %.2f, %.2f, %.2f, %lld\n",
				m_snap[i].m_symbol,
				m_snap[i].m_quote_update_time,
				m_snap[i].m_local_time,
				m_snap[i].m_bid_lev1.m_price,
				m_snap[i].m_bid_lev1.m_qty,
				m_snap[i].m_ask_lev1.m_price,
				m_snap[i].m_ask_lev1.m_qty,
				m_snap[i].m_bid_lev10.m_price,
				m_snap[i].m_bid_lev10.m_qty,
				m_snap[i].m_ask_lev10.m_price,
				m_snap[i].m_ask_lev10.m_qty,
				m_snap[i].m_total_bid_qty,
				m_snap[i].m_total_ask_qty,
				m_snap[i].m_open_price,
				m_snap[i].m_day_high,
				m_snap[i].m_day_low,
				m_snap[i].m_last_price,
				m_snap[i].m_total_qty);

			fwrite(ch_buffer, strlen(ch_buffer), 1, fp);
		}
	}
	fflush( fp );
	
	fclose( fp );
}


