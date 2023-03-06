//
// Created by Administrator on 2020/7/2.
//

#include <thread>
#include <cstring>
#include "EngineContext.h"
#include "CtpTdGateway.h"

int CtpTdGateway::nRequestID=0;
int CtpTdGateway::connect()
{
    void *handle = dlopen("../lib/ctp/thosttraderapi_se.so",RTLD_LAZY);
    if (handle != nullptr)
    {
        typedef CThostFtdcTraderApi* (*CreateFtdcTdApiFunc)(const char*);
        CreateFtdcTdApiFunc pfnCreateFtdcTdApiFunc = (CreateFtdcTdApiFunc)dlsym(handle, "_ZN19CThostFtdcTraderApi19CreateFtdcTraderApiEPKc");
        if (pfnCreateFtdcTdApiFunc == nullptr)
        {
            Logger::getLogger().info("load thosttraderapi.dll fail [%d] [%s]", errno, strerror(errno));
            return -1;
        }
        m_pUserApi = pfnCreateFtdcTdApiFunc(".");
        m_pUserApi->RegisterSpi(this);
        m_pUserApi->SubscribePrivateTopic(THOST_TERT_QUICK);
        m_pUserApi->SubscribePublicTopic(THOST_TERT_QUICK);
    }
    else
    {
        Logger::getLogger().info("load thosttraderapi.dll fail [%d] [%s]", errno, strerror(errno));
        return -1;
    }
    thread t(&CtpTdGateway::Run, this);
    t.detach();
    return 0;
}

void CtpTdGateway::Run()
{
    const char* address = this->loginInfo.address.c_str();
    m_pUserApi->RegisterFront(const_cast<char*>(address));
    m_pUserApi->Init();
    Logger::getLogger().info("%s ctp connecting...",loginInfo.accoutId.c_str());
    m_pUserApi->Join();
}

//客户端认证
void CtpTdGateway::ReqAuthenticate()
{
}
void CtpTdGateway::ReqUserLogin()
{
}
void CtpTdGateway::ReqUserLogout()
{
}
///请求确认结算单
void CtpTdGateway::ReqSettlementInfoConfirm()
{
}
///用户口令更新请求
void CtpTdGateway::ReqUserPasswordUpdate()
{
}

void CtpTdGateway::OnFrontConnected() {
    Logger::getLogger().info("%s OnFrontConnected",loginInfo.accoutId.c_str());

    CThostFtdcReqUserLoginField reqUserLogin = { 0 };
    strcpy(reqUserLogin.BrokerID, loginInfo.brokerId.c_str());
    strcpy(reqUserLogin.UserID, loginInfo.userId.c_str());
    strcpy(reqUserLogin.Password, loginInfo.password.c_str());
    // 发出登陆请求
    m_pUserApi->ReqUserLogin(&reqUserLogin, nRequestID++);
}

void CtpTdGateway::OnFrontDisconnected(int nReason) {
    Logger::getLogger().info("%s OnFrontDisconnected",loginInfo.accoutId.c_str());
}

void CtpTdGateway::OnRspAuthenticate(CThostFtdcRspAuthenticateField *pRspAuthenticateField, CThostFtdcRspInfoField *pRspInfo,
                                int nRequestID, bool bIsLast) {

}

void CtpTdGateway::OnRspUserLogin(CThostFtdcRspUserLoginField *pRspUserLogin, CThostFtdcRspInfoField *pRspInfo,
                                  int nRequestID, bool bIsLast) {
    Logger::getLogger().info("%s OnRspUserLogin",loginInfo.accoutId.c_str());

}

void
CtpTdGateway::OnRspUserLogout(CThostFtdcUserLogoutField *pUserLogout, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                              bool bIsLast) {

}