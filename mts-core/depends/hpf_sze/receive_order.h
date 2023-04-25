/*!****************************************************************************
	@file   	receive_order.h
	
	@brief		提供接收订单行情
				结构体qt_node_order是在记录订单行情需要用到的行情字段
				通过接收处理方法on_receive_message将收到的数据记录的缓存结构体中
				在程序退出时调用close方法中log_quote方法将行情记录到文件中
******************************************************************************/
#pragma once
#include "socket_multicast.h"

#define QT_ORDER_COUNT				(8000000)

#pragma pack(push, 1)

struct qt_node_order
{
	unsigned int		m_seq;						/// 盛立行情序号
	char				m_symbol[9];				/// 合约
	short				m_channel_no;				/// 频道号
	long long			m_ll_seq_no;				/// 消息记录号
	long long			m_ll_update_time;			/// 行情更新时间
	unsigned long long	m_local_time;				///	本地接收时间
	double				m_i_px;						///	价格
	long long			m_ll_qty;					/// 数量
	char				m_i_side;					/// 方向
	char				m_i_order_type;				/// 订单类型
};

#pragma pack(pop)

class udp_quote_order:public socket_event
{
public:
	udp_quote_order(void);
	~udp_quote_order(void);

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
	/// 行情接收资源
	long long			m_ll_order_count;
	qt_node_order		m_order[QT_ORDER_COUNT];
};

