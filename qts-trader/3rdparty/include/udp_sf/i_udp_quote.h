#pragma once
#include <string>
#include "quote_common.h"

using std::string;

///---------------------------------------------2017/4/5/---------------------------------------------
///盛立接口部分
///----------------------------------------------------------------------------------------------------
struct i_udp_quote_event
{
	virtual ~i_udp_quote_event() {};

	virtual void on_receivec_udp_quote(const char * p_data, int n_len, int sec, int nsec) = 0;
	virtual void on_udp_quote_log_msg(const char * p_data, int n_len) = 0;
};

class sock_udp_param
{
public:
	sock_udp_param();
	~sock_udp_param();
	sock_udp_param(const sock_udp_param& other);
	sock_udp_param& operator= (const sock_udp_param& other);

private:
	void assign(const sock_udp_param& other);

public:
	string			m_efh_udp_ip;
	unsigned short	m_efh_udp_port;
	string			m_local_ip;
	unsigned short	m_local_port;
	string			m_eth_name;
	int				m_cpu_id;
};

class i_udp_quote
{
public:
	static i_udp_quote *create_udp_quote();
	virtual ~i_udp_quote(void) {}

	virtual bool init(sock_udp_param &shfe, i_udp_quote_event* p_event) = 0;
	virtual void close() = 0;
};