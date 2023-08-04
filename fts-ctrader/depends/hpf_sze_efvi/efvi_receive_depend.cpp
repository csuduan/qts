#include "efvi_receive_depend.h"

sock_udp_param::sock_udp_param()
{
	m_i_cpu_id		= 1;
}

sock_udp_param::sock_udp_param(const sock_udp_param& other)
{
	assign(other);
}

sock_udp_param::~sock_udp_param()
{
}

sock_udp_param& sock_udp_param::operator=(const sock_udp_param& other)
{
	if (&other == this)
	{
		return *this;
	}

	assign(other);

	return *this;
}

void sock_udp_param::assign(const sock_udp_param& other)
{
	m_i_cpu_id			= other.m_i_cpu_id;
	m_multicast_ip		= other.m_multicast_ip;
	m_multicast_port	= other.m_multicast_port;
	m_eth_name			= other.m_eth_name;
}