/*!****************************************************************************
	@file   	efvi_receive_snap.h
	
	@brief		提供接收lev2行情
				结构体qt_node_snap是在记录lev2行情需要用到的行情字段
				通过接收处理方法handle_rx将收到的数据记录的缓存结构体中
				在程序退出时调用close方法中log_quote方法将行情记录到文件中
******************************************************************************/
#pragma once
#include "efvi_receive_depend.h"

#define QT_SNAP_COUNT				(8000000)

#pragma pack(push, 1)
struct qt_px_qty_unit
{
	double				m_price;				/// 价格
	long long			m_qty;					/// 数量
};

struct qt_node_snap
{
	unsigned int		m_seq;						/// 盛立行情序号
	char				m_symbol[9];			/// 合约
	long long			m_quote_update_time;	/// 行情更新时间
	unsigned long long	m_local_time;			/// 本地接收时间
	// snp
	qt_px_qty_unit		m_bid_lev1;				/// 买向第一档
	qt_px_qty_unit		m_bid_lev10;			/// 买向第十档
	qt_px_qty_unit		m_ask_lev1;				/// 卖向第一档
	qt_px_qty_unit		m_ask_lev10;			/// 卖向第十档
	double				m_open_price;			/// 开盘价
	double				m_day_high;				/// 最高价
	double				m_day_low;				/// 最低价
	double				m_last_price;			/// 最新价
	long long			m_total_qty;			/// 成交量
	long long			m_total_bid_qty;		/// 买入汇总
	long long			m_total_ask_qty;		/// 卖出汇总
};

#pragma pack(pop)

class udp_quote_snap
{
public:
	udp_quote_snap(void);
	~udp_quote_snap(void);

	bool init( sock_udp_param &shfe );
	void close();
private:
	int sl_parse(ef_filter_spec* fs, const char* ip,  unsigned short port);
	struct pkt_buf* pkt_buf_from_id(struct resources* res, int pkt_buf_i);
	void pkt_buf_free(struct resources* res, struct pkt_buf* pkt_buf);
	void handle_rx(struct resources* res, int pkt_buf_i, int len);
	void handle_rx_discard(struct resources* res, int pkt_buf_i, int len, int discard_type);
	void refill_rx_ring(struct resources* res);

	/// 创建线程
	bool start_recv_thread(); 
	static void* func_work_thread(void* param);
	int on_work_thread();
	void thread_main_loop(struct resources* res);
	/// 记录日志功能
	void write_log(const char* msg);
	/// 绑定CPU
	bool bind_cpu(int cpu_id, pthread_t thd_id);
	/// 记录行情内容
	void log_quote();
private:
	sock_udp_param		m_udp_param;			///<组播参数
	bool				m_receive_quit_flag;	///<退出线程的标志	
	struct resources*	m_res;					///<行情资源

	/// 行情接收资源
	long long			m_ll_snap_count;
	qt_node_snap			m_snap[QT_SNAP_COUNT];
};

