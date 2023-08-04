/*!****************************************************************************
 @note   Copyright (coffee), 2005-2015, Shengli Tech. Co., Ltd.
 @file   socket_multicast.h
 @date   2015/4/29   17:17
 @author zhou.hu

 @brief     本类主要实现组播数据的接收，依赖solarflare的openonlad底层库来实现

 @note
******************************************************************************/

#pragma once


#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <sys/epoll.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include <iostream>

#include <string>
#include <map>
#include "udp_sf/i_udp_quote.h"

using std::string;
using std::map;



#define MY_SOCKET							int

#define SL_SOCK_DEFAULT						0
#define SL_SOCK_SEND						1
#define SL_SOCK_RECV						2
#define SL_SOCK_ERROR						3

///socket文件描述符缺省值
#define MY_SOCKET_DEFAULT					-1
///socket错误信息
#define MY_SOCKET_ERROR						-1
///最大的接收缓冲区最
#define	RCV_BUF_SIZE						65535
///服务器端最大的支持的客户端的连接数
#define MAX_SOCKET_CONNECT					1024

namespace multicast{
    ///-----------------------------------------------------------------------------
///回调参数事件
///-----------------------------------------------------------------------------
    enum SOCKET_EVENT
    {
        EVENT_CONNECT,				//连接成功事件
        EVENT_REMOTE_DISCONNECT,	//联接端断开事件
        EVENT_LOCALE_DISCONNECT,	//主动断开事件
        EVENT_NETWORK_ERROR,		//网络错误
        EVENT_RECEIVE,				//数据接收事件
        EVENT_SEND,					//数据发送结束事件
        EVENT_RECEIVE_BUFF_FULL,	//接收缓冲区满
        EVENT_UNKNOW,				//未定义状态
    };

    class socket_event
    {
    public:
        virtual ~socket_event() {}
        /// \brief 接收到组播数据的回詷事件
        virtual void on_receive_message(int id, const char* buff, unsigned int len) = 0;
    };


    class socket_multicast : i_udp_quote_event
    {
    public:
        socket_multicast(){

            m_thrade_quit_flag = false;

            m_id = 0;
            m_remote_port = 0;

            m_event = NULL;
            m_sock = NULL;

        }
        virtual ~socket_multicast(void){

        }
        /// \brief 组播实例初始化
        bool sock_init(const string& remote_ip, unsigned short remote_port, const string& local_eth, int id, socket_event* ptr_event){
            m_remote_ip = remote_ip;
            m_remote_port = remote_port;
            m_local_eth = local_eth;
            m_id = id;
            m_event = ptr_event;

            m_sock = i_udp_quote::create_udp_quote();
            if (!m_sock)
            {
                fprintf(stderr, "create i_udp_quote recver error!");
                return false;
            }

            //接收的组播ip
            m_sock_param.m_efh_udp_ip = remote_ip;
            //接收的组播端口
            m_sock_param.m_efh_udp_port = remote_port;
            //接收的本机网卡
            m_sock_param.m_eth_name = m_local_eth;
            //绑定cpu， 如果为-1 表示不绑定
            m_sock_param.m_cpu_id = -1;

            return m_sock->init(m_sock_param, this);
        }
        /// \brief 组播实例关闭
        bool sock_close(){
            if (m_sock)
                delete m_sock;

            return true;
        }

        virtual void on_receivec_udp_quote(const char * p_data, int n_len, int sec, int nsec){
            report_user(EVENT_RECEIVE, m_id, p_data, n_len);

        }
        virtual void on_udp_quote_log_msg(const char * p_data, int n_len){}


    protected:
        //----------------------------------------------------------------------------
        //保护类成员函数
        //----------------------------------------------------------------------------

        /// \brief 向客户报告的回调事件
        bool report_user(SOCKET_EVENT eventType, int id, const char *buff, unsigned int size){
            if(NULL == m_event)
            {
                return false;
            }

            switch(eventType)
            {
                case  EVENT_RECEIVE:
                {
                    m_event->on_receive_message(id, buff, size);
                }
                    break;
                default:
                    break;
            }
            return true;
        }

        /// \brief 日志记录接口
        void log_msg(const string& msg){
            std::cout << msg << std::endl;
        }

    protected:
        socket_event*			m_event;				///< 回调接口
        bool					m_thrade_quit_flag;		///< 信号检测线程退出标志

        string					m_remote_ip;			///< 组播IP
        unsigned short			m_remote_port;			///< 组播端口
        string					m_local_eth;			///< 本地网卡
        int						m_id;					///< 连接编号
        class i_udp_quote		*m_sock;				///< udp接受接口
        class sock_udp_param	m_sock_param;			///< udp接受接口参数
    };
}








