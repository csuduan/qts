/*!****************************************************************************
	@file   	efvi_receive_ts.h
	
	@brief		提供接收timesale行情
				结构体qt_node_exe是在记录timesale行情需要用到的行情字段
				通过接收处理方法handle_rx将收到的数据记录的缓存结构体中
				在程序退出时调用close方法中log_quote方法将行情记录到文件中
******************************************************************************/
#pragma once
#include "efvi_receive_depend.h"

#define QT_TS_COUNT				(8000000)

#pragma pack(push, 1)

struct qt_node_exe
{
	unsigned int			m_seq;					/// 盛立行情序号
	char					m_symbol[9];			/// 合约
	short					m_channel_no;			/// 频道号
	long long				m_ll_seq_no;			/// 消息记录号
	long long				m_ll_update_time;		/// 行情时间
	unsigned long long		m_local_time;			/// 本地接收时间
	double					m_i_exe_px;				/// 成交价格
	long long				m_ll_exe_qty;			/// 成交量
	char					m_i_exe_type;			/// 成交类型
};

#pragma pack(pop)

class udp_quote_ts
{
public:
	udp_quote_ts(void);
	~udp_quote_ts(void);

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

	/// 行情接收记录缓冲字段
	long long			m_ll_exe_count;
	qt_node_exe			m_exe[QT_TS_COUNT];	
};

