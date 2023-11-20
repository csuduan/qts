#pragma once

#include <stdint.h>

#pragma pack(push, 1)
/// add by zhou.hu review 2016/9/7 MAC头
struct mac_head
{
public:
	uint8_t		m_dst_addr[6];
	uint8_t		m_src_addr[6];
	uint16_t	m_type;
public:
	void swap();
};

/// add by zhou.hu review 2016/9/7 IP头
union ip_version
{
	uint8_t		m_ver_and_len;
	struct
	{
		uint8_t	m_head_len : 4;
		uint8_t	m_version : 4;
	};
};

union ip_offset
{
	uint16_t		m_slice_and_offset;
	struct
	{
		uint16_t	m_slice : 3;
		uint16_t	m_offset : 13;
	};
};

union ip_addr
{
	uint32_t		m_ip;
	struct
	{
		uint8_t		m_b[4];
	};
};

struct ip_head
{
	ip_version				m_version;				///<版本与首部长度
	uint8_t					m_tos;					///<8位服务类型
	uint16_t				m_total_len;			///<16位总长度
	uint16_t				m_packet_id;			///<16位标识
	ip_offset				m_offset;				///<3位标志信息及以13位偏移
	uint8_t					m_ttl;					///<8位生成时间(TTL)
	uint8_t					m_protocol;				///<8位协议
	uint16_t				m_check_sum;			///<16位首部校验和
	ip_addr					m_src_ip;				///<32位源地址
	ip_addr					m_dst_ip;				///<32位目标地址

public:
	void swap();
};


class udp_head
{
public:
	uint16_t		m_src_port;			///< 源端口
	uint16_t		m_dst_port;			///< 目标端口
	uint16_t		m_udp_len;			///< UDP长度
	uint16_t		m_crc;				///< 检验和
public:
	void swap();
};

struct tcp_head
{
	unsigned short	m_src_port;					//源端口 16bit
	unsigned short	m_dst_port;					//目的端口 16bit
	unsigned int	m_ui_seq_num;				//序列号 32bit
	unsigned int	m_ui_ack_num;				//确认号 32bit
	unsigned short	m_head_len_and_flag;		//前4位 tcp头长度, 中6位保留， 后6位标志位
	unsigned short	m_window_size;				//窗口大小16bit
	unsigned short	m_check_sum;				//校验和 16bit
	unsigned short	m_surgent_pointer;			//紧急数据偏移 16bit
};

#pragma pack(pop)
