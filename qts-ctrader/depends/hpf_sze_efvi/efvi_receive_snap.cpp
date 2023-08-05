#include <iostream>
#include <sstream>
#include <algorithm>
#include <cmath>
#include <vector>
#include <time.h>
#include "efvi_receive_snap.h"
#include "sze_hpf_define.h"

using namespace std;

udp_quote_snap::udp_quote_snap(void)
{
	m_res			= NULL;
}

udp_quote_snap::~udp_quote_snap(void)
{
}

//***************************************************************************
//*********************重点接收处理level2行情部分代码************************
//***************************************************************************
void udp_quote_snap::handle_rx(struct resources* res, int pkt_buf_i, int len)
{
	struct pkt_buf* pkt_buf = pkt_buf_from_id(res, pkt_buf_i);
	const char* ptr			=  (const char*)(pkt_buf->rx_ptr);

	/***************网络处理**********************/
	/// 网络包头
	ip_head* p_ip			= (ip_head*)(ptr+sizeof(mac_head));
	/// ip头长度
	int ip_head_total_len	= p_ip->m_version.m_head_len * 4;
	udp_head* p_udp			= (udp_head*)(ptr + sizeof(mac_head) + ip_head_total_len );
	/// udp数据长度
	int udp_len				= ntohs(p_udp->m_udp_len) - sizeof(udp_head);
	int net_header_len		= ip_head_total_len + (int)sizeof(mac_head) + (int)sizeof(udp_head);

	/// 数据处理部分
	const char*	ptr_udp		= ptr + net_header_len;
	int	remain_len			= udp_len;
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

		m_snap[node].m_seq		= p_snp->m_header.m_sequence;
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

	pkt_buf_free(res, pkt_buf);
}

bool udp_quote_snap::init(sock_udp_param &shfe)
{
	m_udp_param			= shfe;
	m_receive_quit_flag = false;

	m_res = (resources*)calloc(1, sizeof(struct resources));
	if (!m_res)
	{
		exit(1);
	}

	/* Open driver and allocate a VI. */
	TRY(ef_driver_open(&m_res->dh));

	TRY(ef_pd_alloc_by_name(&m_res->pd, m_res->dh, m_udp_param.m_eth_name.c_str(), EF_PD_DEFAULT));
  
	TRY(ef_vi_alloc_from_pd(&m_res->vi, m_res->dh, &m_res->pd, m_res->dh,-1, 2048, 0, NULL, -1, EF_VI_FLAGS_DEFAULT));

	m_res->rx_prefix_len = ef_vi_receive_prefix_len(&m_res->vi);

	//LOGI("rxq_size=%d\n", ef_vi_receive_capacity(&m_res->vi));
	//LOGI("evq_size=%d\n", ef_eventq_capacity(&m_res->vi));
	//LOGI("rx_prefix_len=%d\n", m_res->rx_prefix_len);

	//ef_vi_receive_set_buffer_len(&m_res->vi,512*1024);

	/* Allocate memory for DMA transfers. Try mmap() with MAP_HUGETLB to get huge
	* pages. If that fails, fall back to posix_memalign() and hope that we do
	* get them. */
	m_res->pkt_bufs_n = ef_vi_receive_capacity(&m_res->vi);
	size_t alloc_size = m_res->pkt_bufs_n * PKT_BUF_SIZE;
	alloc_size = ROUND_UP(alloc_size, huge_page_size);
	m_res->pkt_bufs = mmap(NULL, alloc_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_HUGETLB, -1, 0);
	if( m_res->pkt_bufs == MAP_FAILED ) 
	{
		//LOGW("mmap() failed. Are huge pages configured?\n");

		/* Allocate huge-page-aligned memory to give best chance of allocating
		* transparent huge-pages.
		*/
		TEST(posix_memalign(&m_res->pkt_bufs, huge_page_size, alloc_size) == 0);
	}

	int i;
	for( i = 0; i < m_res->pkt_bufs_n; ++i ) 
	{
		struct pkt_buf* pkt_buf = pkt_buf_from_id(m_res, i);
		pkt_buf->rx_ptr = (char*) pkt_buf + RX_DMA_OFF + m_res->rx_prefix_len;
		pkt_buf->id = i;
		pkt_buf_free(m_res, pkt_buf);
	}

	/* Register the memory so that the adapter can access it. */
	TRY(ef_memreg_alloc(&m_res->memreg, m_res->dh, &m_res->pd, m_res->dh,m_res->pkt_bufs, alloc_size));

	for( i = 0; i < m_res->pkt_bufs_n; ++i ) 
	{
		struct pkt_buf* pkt_buf = pkt_buf_from_id(m_res, i);
		pkt_buf->m_ef_addr = ef_memreg_dma_addr(&m_res->memreg, i * PKT_BUF_SIZE);
	}

	/* Fill the RX ring. */
	while( ef_vi_receive_space(&m_res->vi) > REFILL_BATCH_SIZE )
		refill_rx_ring(m_res);

	/* Add filters so that adapter will send packets to this VI. */
	ef_filter_spec guava_80;
	if( sl_parse(&guava_80, m_udp_param.m_multicast_ip.c_str(), m_udp_param.m_multicast_port) != 0 ) 
	{
		// LOGE("ERROR: Bad filter spec '%s'\n", argv[0]);
		exit(1);
	}
	TRY(ef_vi_filter_add(&m_res->vi, m_res->dh, &guava_80, NULL));

	/// 初始化内存
	for (int i = 0; i < QT_SNAP_COUNT; i++)
	{
		memset(&m_snap[i], 0, sizeof(qt_node_snap));
	}

	/// 开始接收线程
	bool ret = start_recv_thread();

	return ret;
}

void udp_quote_snap::close()
{
	m_receive_quit_flag = true;
	sleep(10);
	log_quote();
}

int udp_quote_snap::sl_parse(ef_filter_spec* fs, const char* ip, unsigned short port)
{
	ef_filter_spec_init(fs, EF_FILTER_FLAG_NONE);

	sockaddr_in host_addr;

	bzero(&host_addr, sizeof(host_addr));
	host_addr.sin_family = AF_INET;	

	inet_aton(ip,&(host_addr.sin_addr));
	host_addr.sin_port = htons(port);

	TRY(ef_filter_spec_set_ip4_local(fs, IPPROTO_UDP, host_addr.sin_addr.s_addr, host_addr.sin_port));

	return 0;
}

struct pkt_buf* udp_quote_snap::pkt_buf_from_id(struct resources* res, int pkt_buf_i)
{
	assert((unsigned) pkt_buf_i < (unsigned) res->pkt_bufs_n);
	return (pkt_buf*) ((char*) res->pkt_bufs + (size_t) pkt_buf_i * PKT_BUF_SIZE);
}

void udp_quote_snap::pkt_buf_free(struct resources* res, struct pkt_buf* pkt_buf)
{
	pkt_buf->next = res->free_pkt_bufs;
	res->free_pkt_bufs = pkt_buf;
	++(res->free_pkt_bufs_n);
}

void udp_quote_snap::handle_rx_discard(struct resources* res, int pkt_buf_i, int len, int discard_type)
{
	struct pkt_buf* pkt_buf;

	LOGE("ERROR: discard type=%d\n", discard_type);

	if( /* accept_discard_pkts */ 1 ) 
	{
		handle_rx(res, pkt_buf_i, len);
	}
	else 
	{
		pkt_buf = pkt_buf_from_id(res, pkt_buf_i);
		pkt_buf_free(res, pkt_buf);
	}
}

void udp_quote_snap::refill_rx_ring(struct resources* res)
{
	struct pkt_buf* pkt_buf;
	int i;

	if( ef_vi_receive_space(&res->vi) < REFILL_BATCH_SIZE || res->free_pkt_bufs_n < REFILL_BATCH_SIZE )
		return;

	for( i = 0; i < REFILL_BATCH_SIZE; ++i ) 
	{
		pkt_buf = res->free_pkt_bufs;
		res->free_pkt_bufs = res->free_pkt_bufs->next;
		--(res->free_pkt_bufs_n);
		ef_vi_receive_init(&res->vi, pkt_buf->m_ef_addr + RX_DMA_OFF, pkt_buf->id);
	}
	ef_vi_receive_push(&res->vi);
}

void udp_quote_snap::thread_main_loop(struct resources* res)
{
	ef_event evs[32];
	//ef_request_id ids[EF_VI_RECEIVE_BATCH];
	int i, n_ev;

	while( !m_receive_quit_flag ) 
	{
		/* Poll event queue for events */
		n_ev = ef_eventq_poll(&res->vi, evs, sizeof(evs) / sizeof(evs[0]));
		if( n_ev > 0 ) 
		{
			for( i = 0; i < n_ev; ++i ) 
			{
				switch( EF_EVENT_TYPE(evs[i]) ) 
				{
				case EF_EVENT_TYPE_RX:
					/* This code does not handle jumbos. */
					assert(EF_EVENT_RX_SOP(evs[i]) != 0);
					assert(EF_EVENT_RX_CONT(evs[i]) == 0);
					handle_rx(res, EF_EVENT_RX_RQ_ID(evs[i]),
						EF_EVENT_RX_BYTES(evs[i]) - res->rx_prefix_len);
					break;
				case EF_EVENT_TYPE_RX_DISCARD:
					handle_rx_discard(res, EF_EVENT_RX_DISCARD_RQ_ID(evs[i]),
						EF_EVENT_RX_DISCARD_BYTES(evs[i]) - res->rx_prefix_len,
						EF_EVENT_RX_DISCARD_TYPE(evs[i]));
					break;
				default:
					LOGE("ERROR: unexpected event type=%d\n", (int) EF_EVENT_TYPE(evs[i]));
					break;
				}
			}
			refill_rx_ring(res);
		}
	}
}


void* udp_quote_snap::func_work_thread(void* param)
{
	udp_quote_snap* ptr = (udp_quote_snap*) param;
	ptr->on_work_thread();

	return 0;
}

int udp_quote_snap::on_work_thread()
{
	bind_cpu( m_udp_param.m_i_cpu_id, pthread_self() );
	thread_main_loop(m_res);

	return 0;
}

void udp_quote_snap::write_log(const char* msg)
{
	cout << msg << endl;
}

bool udp_quote_snap::bind_cpu( int cpu_id, pthread_t thd_id )
{
	int			cpu = (int)sysconf(_SC_NPROCESSORS_ONLN);
	cpu_set_t	cpu_info;

	if( cpu < cpu_id )
	{
		return false;
	}

	CPU_ZERO(&cpu_info);
	CPU_SET(cpu_id,&cpu_info);

	if( pthread_setaffinity_np( thd_id, sizeof(cpu_set_t), &cpu_info ) != 0 )
	{
		return false;
	}

	return true;
}

bool udp_quote_snap::start_recv_thread()
{
	pthread_t	id;
	pthread_attr_t threadAttr ;
	pthread_attr_init( & threadAttr );
	pthread_attr_setdetachstate( & threadAttr , PTHREAD_CREATE_DETACHED);

	if ( 0 != pthread_create(&id, &threadAttr, udp_quote_snap::func_work_thread, (void*)(this)) )
	{
		pthread_attr_destroy( &threadAttr );
		id = 0 ;
		return false;
	}
	pthread_attr_destroy( &threadAttr );

	return true;
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
			sprintf(ch_buffer, "%u,%s, %lld, %lld, %.2f, %lld, %.2f, %lld, %.2f, %lld, %.2f, %lld, %lld, %lld, %.2f, %.2f, %.2f, %.2f, %lld\n",
				m_snap[i].m_seq,
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


