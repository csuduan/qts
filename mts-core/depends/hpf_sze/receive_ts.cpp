#include <iostream>
#include <sstream>
#include <algorithm>
#include <cmath>
#include <vector>
#include <time.h>
#include "receive_ts.h"
#include "sze_hpf_define.h"

using namespace std;

udp_quote_ts::udp_quote_ts(void)
{
}

udp_quote_ts::~udp_quote_ts(void)
{
}

//***************************************************************************
//*********************重点接收处理timesale行情部分代码**********************
//***************************************************************************
void udp_quote_ts::on_receive_message(int id, const char* buff, unsigned int len)
{
	/// 数据处理部分
	const char*	ptr_udp		= buff;
	int	remain_len			= len;
	for (; remain_len > 0;)
	{
		sze_hpf_exe_pkt* p_exec = (sze_hpf_exe_pkt*)(ptr_udp);

		timespec					ts;
		clock_gettime(CLOCK_REALTIME, &ts);

		int node = m_ll_exe_count % QT_TS_COUNT;
		memcpy(m_exe[node].m_symbol, p_exec->m_header.m_symbol, 9);
		m_exe[node].m_seq			= p_exec->m_header.m_sequence;
		m_exe[node].m_local_time	= ((long long)(ts.tv_sec)) * 1000000000 + ts.tv_nsec;
		m_exe[node].m_channel_no	= p_exec->m_header.m_channel_no;
		m_exe[node].m_ll_seq_no		= p_exec->m_header.m_sequence_num;
		m_exe[node].m_ll_update_time = p_exec->m_header.m_quote_update_time;
		m_exe[node].m_i_exe_px		= (double)(p_exec->m_exe_px) / 10000;
		m_exe[node].m_ll_exe_qty	= p_exec->m_exe_qty / 100;
		m_exe[node].m_i_exe_type	= p_exec->m_exe_type;

		++m_ll_exe_count;
		remain_len -= sizeof(sze_hpf_exe_pkt);
		ptr_udp += sizeof(sze_hpf_exe_pkt);

	}
	/// 数据处理完成
	/***************网络处理完成**********************/
}

bool udp_quote_ts::init( const char* remote_ip, unsigned short remote_port,const char* local_ip, unsigned short local_port )
{
	bool ret = m_sock.sock_init( remote_ip, remote_port, local_ip, local_port, 2, static_cast<socket_event*>(this) );
	if( !ret )
	{
		return false;
	}

	/// 初始化内存
	for (int i = 0; i < QT_TS_COUNT; i++)
	{
		memset(&m_exe[i], 0, sizeof(qt_node_exe));
	}

	return ret;
}

void udp_quote_ts::close()
{
	m_sock.sock_close();
	sleep(10);
	log_quote();
}

void udp_quote_ts::write_log(const char* msg)
{
	cout << msg << endl;
}

void udp_quote_ts::log_quote()
{
	time_t now		= time(NULL);
	tm* ltm			= localtime(&now);
	
	char	str_full_name[1024];
	memset( str_full_name, 0, sizeof(str_full_name) );
	sprintf( str_full_name, "%04d%02d%02d_ts.csv", ltm->tm_year + 1900, ltm->tm_mon + 1, ltm->tm_mday );

	FILE* fp		= fopen(str_full_name, "at+");
	if( fp == NULL )
	{
		return;
	}

	if (m_ll_exe_count > 0)
	{
		for (int i = 0; i < QT_TS_COUNT; i++)
		{
			if (m_exe[i].m_local_time == 0)
			{
				break;
			}

			char ch_buffer[1024];
			memset(ch_buffer, 0, sizeof(ch_buffer));
			sprintf(ch_buffer, "%s, %u, %lld, %lld, %lld, %.2f, %lld, %c\n",
				m_exe[i].m_symbol,
				m_exe[i].m_channel_no,
				m_exe[i].m_ll_seq_no,
				m_exe[i].m_ll_update_time,
				m_exe[i].m_local_time,
				m_exe[i].m_i_exe_px,
				m_exe[i].m_ll_exe_qty,
				m_exe[i].m_i_exe_type );

			fwrite(ch_buffer, strlen(ch_buffer), 1, fp);
		}
	}
	fflush( fp );
	
	fclose( fp );
}


