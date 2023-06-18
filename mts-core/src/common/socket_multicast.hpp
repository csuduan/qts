/*!****************************************************************************
 @note   Copyright (coffee), 2005-2015, Shengli Tech. Co., Ltd.
 @file   socket_multicast.h
 @date   2015/4/29   17:17
 @author zhou.hu

 @brief     本类主要实现组播数据的接收， 在原来的基础上略作改进，不再支持windows平台。
		用户在具体实现时可以采用自己的UDP收发工具来替代此类的代码。

 @note
******************************************************************************/

#pragma once


#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <sys/epoll.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/types.h>
#include <bits/socket.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <pthread.h>
#include <asm/errno.h>

#include <iostream>
#include <sstream>
#include <string>
#include <assert.h>
#include <errno.h>

#include <string>
#include <map>

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


    class socket_multicast
    {
    public:
        socket_multicast(){
            m_thrade_quit_flag = false;
            m_id = 0;
            m_local_port = 0;
            m_remote_port = 0;
            m_event = NULL;
            m_sock = MY_SOCKET_DEFAULT;
        }
        virtual ~socket_multicast(void){

        }
        /// \brief 组播实例初始化
        bool sock_init(const string& remote_ip, unsigned short remote_port,const string& local_ip, unsigned short local_port, int id, socket_event* ptr_event){
            bool b_ret = false;
            const int CONST_ERROR_SOCK = -1;

            m_remote_ip = remote_ip;
            m_remote_port = remote_port;
            m_local_ip = local_ip;
            m_local_port = local_port;
            m_id = id;
            m_event = ptr_event;

            try
            {
                m_sock = socket(PF_INET, SOCK_DGRAM, 0);
                if(MY_SOCKET_ERROR == m_sock)
                {
                    throw CONST_ERROR_SOCK;
                }

                //socket可以重新使用一个本地地址
                int flag=1;
                if(setsockopt(m_sock, SOL_SOCKET, SO_REUSEADDR, (const char*)&flag, sizeof(flag)) != 0)
                {
                    throw CONST_ERROR_SOCK;
                }

                int options = fcntl(m_sock, F_GETFL);
                if(options < 0)
                {
                    throw CONST_ERROR_SOCK;
                }
                options = options | O_NONBLOCK;
                int i_ret = fcntl(m_sock, F_SETFL, options);
                if(i_ret < 0)
                {
                    throw CONST_ERROR_SOCK;
                }

                struct sockaddr_in local_addr;
                memset(&local_addr, 0, sizeof(local_addr));
                local_addr.sin_family = AF_INET;
                local_addr.sin_addr.s_addr = htonl(INADDR_ANY);
                local_addr.sin_port = htons(m_remote_port);	//multicast port
                if (bind(m_sock, (struct sockaddr*)&local_addr, sizeof(local_addr)) < 0)
                {
                    throw CONST_ERROR_SOCK;
                }

                struct ip_mreq mreq;
                mreq.imr_multiaddr.s_addr = inet_addr(m_remote_ip.c_str());	//multicast group ip
                mreq.imr_interface.s_addr = inet_addr(m_local_ip.c_str());

                if (setsockopt(m_sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq)) != 0)
                {
                    throw CONST_ERROR_SOCK;
                }

                int receive_buf_size  = RCV_BUF_SIZE;
                if (setsockopt(m_sock, SOL_SOCKET, SO_RCVBUF, (const char*)&receive_buf_size, sizeof(receive_buf_size)) != 0)
                {
                    throw CONST_ERROR_SOCK;
                }

                //启动线程
                b_ret = start_server_event_thread();
            }
            catch(...)
            {
                close(m_sock);
                b_ret = false;
            }

            return b_ret;
        }
        /// \brief 组播实例关闭
        bool sock_close(){
            bool b_ret = false;
            //关闭线程
            b_ret = stop_server_event_thread();

            if (m_sock != MY_SOCKET_DEFAULT)
            {
                close(m_sock);
                m_sock = MY_SOCKET_DEFAULT;
            }

            return b_ret;
        }

    protected:
        //----------------------------------------------------------------------------
        //保护类成员函数
        //----------------------------------------------------------------------------


        /// \brief 组播数收发信号的线程函数(linux 版)
        static void* socket_server_event_thread(void* ptr_param){
            socket_multicast* ptr_this = (socket_multicast*) ptr_param;
            if (NULL == ptr_this)
            {
                return NULL;
            }

            return ptr_this->on_socket_server_event_thread();
        }

        /// \brief 组播数收发信号的处理函数
        void* on_socket_server_event_thread(){
            char line[RCV_BUF_SIZE] = "";

            int n_rcved = -1;

            struct sockaddr_in muticast_addr;

            memset(&muticast_addr, 0, sizeof(muticast_addr));
            muticast_addr.sin_family = AF_INET;
            muticast_addr.sin_addr.s_addr = inet_addr(m_remote_ip.c_str());
            muticast_addr.sin_port = htons(m_remote_port);

            while (true)
            {
                socklen_t len = sizeof(sockaddr_in);

                n_rcved = recvfrom(m_sock, line, RCV_BUF_SIZE, 0, (struct sockaddr*)&muticast_addr, &len);
                if ( n_rcved < 0)
                {
                    continue;
                }
                else if (0 == n_rcved)
                {
                    continue;
                }
                else
                {
                    report_user(EVENT_RECEIVE, m_id, line, n_rcved);
                }

                //检测线程退出信号
                if (m_thrade_quit_flag)
                {
                    //此时已关闭完所有的客户端
                    return NULL;
                }
            }

            return NULL;
        }

        /// \brief 启动组播信号处理线程
        bool start_server_event_thread(){
            m_thrade_quit_flag = false;


            pthread_t thread_id;
            pthread_attr_t thread_attr;
            pthread_attr_init(&thread_attr);
            pthread_attr_setdetachstate(&thread_attr, PTHREAD_CREATE_DETACHED);		///<设置线程可分离
            //pthread_attr_setinheritsched(&thread_attr, PTHREAD_EXPLICIT_SCHED);		///<设置线程的继承策略和参数来自于schedpolicy 与 schedparam中属性中显示设置
            //pthread_attr_setscope(&thread_attr, PTHREAD_SCOPE_SYSTEM);				///<设置线程的与系统中所有线程进行竞争

            //pthread_attr_setschedpolicy(&thread_attr, SCHED_FIFO);					///<设置线程的调试策略
            //int max_priority = sched_get_priority_max(SCHED_FIFO);					///<取得最大的优先级
            ////int min_priority = sched_get_priority_min(SCHED_FIFO);				///<取得最小的优先级

            //struct sched_param sched_value;
            //sched_value.sched_priority = max_priority;
            //pthread_attr_setschedparam(&thread_attr, &sched_value);					///<设置优先级

            int ret = pthread_create(&thread_id, &thread_attr, socket_server_event_thread, this);
            pthread_attr_destroy(&thread_attr);

            if (ret != 0)
            {
                return false;
            }

            return true;
        }
        /// \brief 停止组播信号处理线程
        bool stop_server_event_thread(){
            m_thrade_quit_flag = true;

            return true;
        }

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
        string					m_local_ip;				///< 本地IP
        unsigned short			m_local_port;			///< 本地端口
        int						m_id;					///< 连接编号
        MY_SOCKET				m_sock;					///< 套接口
    };
}









