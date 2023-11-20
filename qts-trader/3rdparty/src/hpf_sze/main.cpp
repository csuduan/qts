#include "receive_ts.h"
#include "receive_snap.h"
#include "receive_order.h"

/**
 * 深交所lev2行情组播地址信息如下：
               （1）行情组播地址：233.57.2.100
               （2）组播端口：37200
                可以接收组播数据的本地网段：192.168.66.X，本地端口可以任意指定（不与已有地址冲突即可）。

                深交所idx指数行情组播地址信息如下：
               （3）行情组播地址 ：233.57.2.101
               （4）组播端口：37201
                接收组播数据的网段：192.168.66.X，本地端口可以任意指定（不与已有地址冲突即可）。

                深交所order订单行情组播地址信息如下：
               （5）行情组播地址 ：233.57.2.102
               （6）组播端口：37202
                接收组播数据的网段：192.168.66.X，本地端口可以任意指定（不与已有地址冲突即可）。

                深交所exe成交行情组播地址信息如下：
               （7）行情组播地址 ：233.57.2.102
               （8）组播端口：37202
                接收组播数据的网段：192.168.66.X，本地端口可以任意指定（不与已有地址冲突即可）。

    //            深交所建树行情组播地址信息如下：
    //           （9）行情组播地址 ：233.57.2.104
    //           （10）组播端口：37204
    //            接收组播数据的网段：192.168.66.X，本地端口可以任意指定（不与已有地址冲突即可）。
 * @return
 */

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

	if( !p_recv_ts->init( "233.54.1.100", 20010, "192.168.2.3", 30010 ) )
	{
		printf("timesale receive init failed.\n");
		return -1;
	}

	if( !p_recv_order->init( "233.54.1.101", 20011, "192.168.2.4", 30011 ) )
	{
		printf("order receive init failed.\n");
		return -1;
	}

	if( !p_recv_snap->init( "233.54.1.102", 20012, "192.168.2.5", 30012 ) )
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
