/*!****************************************************************************
	@file   	receive_ts.h
	
	@brief		提供接收timesale行情
				结构体qt_node_exe是在记录timesale行情需要用到的行情字段
				通过接收处理方法on_receive_message将收到的数据记录的缓存结构体中
				在程序退出时调用close方法中log_quote方法将行情记录到文件中
******************************************************************************/
#pragma once
#include "socket_multicast.h"

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

class udp_quote_ts:public socket_event
{
public:
	udp_quote_ts(void);
	~udp_quote_ts(void);

	bool init( const char* remote_ip, unsigned short remote_port,const char* local_ip, unsigned short local_port );
	void close();

	/// \brief 接收到组播数据的回詷事件
	virtual void on_receive_message(int id, const char* buff, unsigned int len);
private:
	/// 记录日志功能
	void write_log(const char* msg);
	/// 记录行情内容
	void log_quote();
private:
	socket_multicast	m_sock;
	/// 行情接收记录缓冲字段
	long long			m_ll_exe_count;
	qt_node_exe			m_exe[QT_TS_COUNT];	
};

