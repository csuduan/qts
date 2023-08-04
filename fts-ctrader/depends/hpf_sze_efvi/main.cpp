#include "efvi_receive_ts.h"
#include "efvi_receive_snap.h"
#include "efvi_receive_order.h"


int main()
{
	udp_quote_ts* p_recv_ts = new udp_quote_ts();
	if( p_recv_ts == NULL )
	{
		printf("malloc udp receive timesale failed.\n");
		return -1;
	}

	udp_quote_order* p_recv_order = new udp_quote_order();
	if( p_recv_order == NULL )
	{
		printf("malloc udp receive order failed.\n");
		delete p_recv_ts;
		p_recv_ts = NULL;
		return -1;
	}

	udp_quote_snap* p_recv_snap = new udp_quote_snap();
	if( p_recv_snap == NULL )
	{
		printf("malloc udp receive snap failed.\n");
		delete p_recv_ts;
		p_recv_ts = NULL;
		delete p_recv_order;
		p_recv_order = NULL;
		return -1;
	}

	sock_udp_param	ts_param, order_param, snap_param;
	ts_param.m_i_cpu_id			= 2;
	ts_param.m_multicast_ip		= "237.1.1.4";
	ts_param.m_multicast_port	= 37400;
	ts_param.m_eth_name			= "p6p1";

	order_param.m_i_cpu_id		= 4;
	order_param.m_multicast_ip	= "237.1.1.3";
	order_param.m_multicast_port= 37300;
	order_param.m_eth_name		= "p6p1";

	snap_param.m_i_cpu_id		= 6;
	snap_param.m_multicast_ip	= "237.1.1.1";
	snap_param.m_multicast_port	= 37100;
	snap_param.m_eth_name		= "p6p1";

	if( !p_recv_ts->init( ts_param) )
	{
		printf("timesale receive init failed.\n");
		return -1;
	}

	if( !p_recv_order->init( order_param ) )
	{
		printf("order receive init failed.\n");
		return -1;
	}

	if( !p_recv_snap->init( snap_param ) )
	{
		printf("lev2 receive init failed.\n");
		return -1;
	}

	while( true )
	{
		printf("[RCV]:");
		fflush( stdout );

		char buf[1024] = {0};
		fgets(buf, sizeof(buf), stdin);
		int len = strlen(buf);
		if( len <= 0 )
		{
			continue;;
		}
		buf[len - 1] = 0;
		printf("\n");

		if( strcmp( buf, "quit" ) == 0 )
		{
			break;
		}
	}

	p_recv_snap->close();
	p_recv_order->close();
	p_recv_ts->close();

	delete p_recv_snap;
	delete p_recv_order;
	delete p_recv_ts;
	p_recv_snap		= NULL;
	p_recv_order	= NULL;
	p_recv_ts		= NULL;

	return 0;
}
