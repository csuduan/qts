#pragma once

#include "UTApiStruct.h"

#if defined(ISLIB) && defined(WIN32)
#define MD_API_EXPORT __declspec(dllexport)
#else
#define MD_API_EXPORT
#endif

class CUTMDSpi
{
public:
    ///当客户端与交易后台建立起通信连接时（还未登录前），该方法被调用。
    virtual void OnFrontConnected(){};
    ///当客户端与交易后台通信连接断开时，该方法被调用。当发生这个情况后，API会自动重新连接，客户端可不做处理。
    virtual void OnFrontDisconnected(int nReason){};
    ///用户登入应答
    virtual void OnRspLogin(CUTRspLoginField *pRspLogin, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {};
    ///请求后台不支持的功能时调用
    virtual void OnRspError(CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast){};
    ///订阅回报
    virtual void OnRspSubDepthMarketData(CUTSubInstrumentField *pSubInstrument, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast){};
    ///退订回报
    virtual void OnRspUnSubDepthMarketData(CUTSubInstrumentField *pSubInstrument, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast){};
    virtual void OnRspSubMDTopic(CUTSubMDTopicField *pSubMDTopic, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {};
    virtual void OnRspUnSubMDTopic(CUTSubMDTopicField *pSubMDTopic, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {};
    virtual void OnRtnDepthMarketData(CUTDepthMarketDataField *pDepthMarketData){};
    virtual void OnRtnL2DepthMarketData(CUTL2DepthMarketDataField *pL2DepthMarketData){};
    virtual void OnRtnL2Order(CUTL2OrderField *pL2Order){};
    virtual void OnRtnL2Trade(CUTL2TradeField *pL2Trade){};
    virtual void OnRtnL2BestOrder(CUTL2BestOrderField *pLeBestOrder){};

};

class MD_API_EXPORT CUTMDApi
{
public:
    ///创建MdApi
    ///@param pszFlowPath 存贮订阅信息文件的目录，默认为当前目录
    ///@return 创建出的UserApi
    ///modify for udp marketdata
    static CUTMDApi *CreateMDApi(const char *pszFlowPath = "",int nCPUID = 0,bool var=true);

    ///获取API的版本信息
    ///@retrun 获取到的版本号
    static const char *GetApiVersion();

    ///删除接口对象本身
    ///@remark 不再使用本接口对象时,调用该函数删除接口对象
    virtual void Release() = 0;

    ///初始化
    ///@remark 初始化运行环境,只有调用后,接口才开始工作
    virtual void Init() = 0;

    ///等待接口线程结束运行
    ///@return 线程退出代码
    virtual int Join();


    ///注册前置机网络地址
    ///@param pszFrontAddress：前置机网络地址。
    ///@remark 网络地址的格式为：“protocol://ipaddress:port”，如：”tcp://127.0.0.1:17001”。
    ///@remark “tcp”代表传输协议，“127.0.0.1”代表服务器地址。”17001”代表服务器端口号。
    virtual void RegisterFront(const char *pszFrontAddress) = 0;

    ///注册回调接口
    ///@param pSpi 派生自回调接口类的实例
    virtual void RegisterSpi(CUTMDSpi *pSpi) = 0;

    ///登录
    virtual int ReqLogin(CUTReqLoginField *pReqLogin, int nRequestID) =0;

    ///退出
    virtual int ReqLogout(CUTReqLogoutField *pReqLoginout, int nRequestID) =0;

    ///订阅行情。
    ///@param ppInstrumentID 合约ID
    ///@param nCount 要订阅/退订行情的合约个数
    ///@remark
    virtual int SubscribeDepthMarketData(CUTSubInstrumentField *pSubInstrument, int nCount) = 0;

    ///退订行情
    virtual int UnSubscribeDepthMarketData(CUTSubInstrumentField *pSubInstrument, int nCount) = 0;

    virtual int SubscribeMDTopic(CUTSubMDTopicField *var1, int var2) =0;
    virtual int UnSubscribeMDTopic(CUTSubMDTopicField var1, int var2)=0;

protected:
    ~CUTMDApi(){};
};