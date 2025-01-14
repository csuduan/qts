#include <iostream>
#include <sstream>
#include <algorithm>
#include <cmath>
#include <vector>
#include <time.h>
#include "receive_order.h"
#include "sze_hpf_define.h"

using namespace std;

udp_quote_order::udp_quote_order(void)
{
}

udp_quote_order::~udp_quote_order(void)
{
}

//***************************************************************************
//*********************重点接收处理order行情部分代码*************************
//***************************************************************************
void udp_quote_order::on_receive_message(int id, const char* buff, unsigned int len)
{
	/// 数据处理部分
	const char*	ptr_udp		= buff;
	int	remain_len			= len;
	for (; remain_len > 0;)
	{
		sze_hpf_order_pkt* p_order = (sze_hpf_order_pkt*)ptr_udp;

		timespec					ts;
		clock_gettime(CLOCK_REALTIME, &ts);

		int node = m_ll_order_count % QT_ORDER_COUNT;
		memcpy(m_order[node].m_symbol, p_order->m_header.m_symbol, 9);
		m_order[node].m_seq		= p_order->m_header.m_sequence;
		m_order[node].m_local_time = ((long long)(ts.tv_sec)) * 1000000000 + ts.tv_nsec;
		m_order[node].m_channel_no = p_order->m_header.m_channel_no;
		m_order[node].m_ll_seq_no = p_order->m_header.m_sequence_num;
		m_order[node].m_ll_update_time = p_order->m_header.m_quote_update_time;
		m_order[node].m_ll_qty = p_order->m_qty/100;
		m_order[node].m_i_px = (double)(p_order->m_px)/10000;
		m_order[node].m_i_order_type = p_order->m_order_type;
		m_order[node].m_i_side = p_order->m_side;

		++m_ll_order_count;
		remain_len -= sizeof(sze_hpf_order_pkt);
		ptr_udp += sizeof(sze_hpf_order_pkt);
	}
	/// 数据处理完成
	/***************网络处理完成**********************/
}

bool udp_quote_order::init(const char* remote_ip, unsigned short remote_port,const char* local_ip, unsigned short local_port)
{
	bool ret = m_sock.sock_init( remote_ip, remote_port, local_ip, local_port, 1, static_cast<socket_event*>(this) );
	if( !ret )
	{
		return false;
	}

	/// 初始化内存
	for (int i = 0; i < QT_ORDER_COUNT; i++)
	{
		memset(&m_order[i], 0, sizeof(qt_node_order));
	}

	return ret;
}

void udp_quote_order::close()
{
	m_sock.sock_close();
	sleep(10);
	log_quote();
}

void udp_quote_order::write_log(const char* msg)
{
	cout << msg << endl;
}

void udp_quote_order::log_quote()
{
	time_t now		= time(NULL);
	tm* ltm			= localtime(&now);
	
	char	str_full_name[1024];
	memset( str_full_name, 0, sizeof(str_full_name) );
	sprintf( str_full_name, "%04d%02d%02d_order.csv", ltm->tm_year + 1900, ltm->tm_mon + 1, ltm->tm_mday );

	FILE* fp		= fopen(str_full_name, "at+");
	if( fp == NULL )
	{
		return;
	}

	if( m_ll_order_count > 0 )
	{
		/// 订单
		for (int i = 0; i < QT_ORDER_COUNT; i++)
		{
			if (m_order[i].m_local_time == 0)
			{
				break;
			}

			char ch_buffer[1024];
			memset(ch_buffer, 0, sizeof(ch_buffer));
			sprintf(ch_buffer, "%s, %u, %lld, %lld, %lld, %.2f, %lld, %c, %c\n",
				m_order[i].m_symbol,
				m_order[i].m_channel_no,
				m_order[i].m_ll_seq_no,
				m_order[i].m_ll_update_time,
				m_order[i].m_local_time,
				m_order[i].m_i_px,
				m_order[i].m_ll_qty,
				m_order[i].m_i_side,
				m_order[i].m_i_order_type);
		
			fwrite( ch_buffer, strlen(ch_buffer), 1, fp );
		}
	}
	fflush( fp );
	
	fclose( fp );
}


