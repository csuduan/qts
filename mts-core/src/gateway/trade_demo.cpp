#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef WINDOWS
#include <Windows.h>
#else
#include <unistd.h>
#endif

#include "ost/UTApi.h"

//以下全局变量需要联系经纪公司进行具体配置
//前置地址
char* g_front_address = "tcp://47.116.77.6:8888";
//用户名
char* g_userid = "00122899";
//密码
char* g_password = "123456";
//下单合约
char g_exchangeid = UT_EXG_SSE;
char* g_instrumentid = "510050";
//价格
double g_price = 3.5;

#ifdef WINDOWS
#define SLEEP_MS(ms) Sleep(ms)
#else
#define SLEEP_MS(ms) usleep(ms*1000)
#endif


int g_reqid = 0;
void gen_order(CUTInputOrderField* pInputOrderField)
{
	memset(pInputOrderField, 0, sizeof(CUTInputOrderField));
	strcpy(pInputOrderField->InvestorID, g_userid);	
	strcpy(pInputOrderField->InstrumentID, g_instrumentid);
	pInputOrderField->ExchangeID = g_exchangeid;
	//OrderRef必须设置,同一会话内必须递增,可以不连续
	pInputOrderField->OrderRef=g_reqid++;
	pInputOrderField->OrderPriceType = UT_OPT_LimitPrice;

	//股票,基金，债券买:HedgeFlag = UT_HF_Speculation,Direction = UT_D_Buy,OffsetFlag = UT_OF_Open
	//股票, 基金，债券卖:HedgeFlag = UT_HF_Speculation, Direction = UT_D_Sell, OffsetFlag = UT_OF_Close
	//债券逆回购:HedgeFlag = UT_HF_Speculation, Direction = UT_D_Sell, OffsetFlag = UT_OF_Open
	//ETF申购:HedgeFlag = UT_HF_Redemption,Direction = UT_D_Buy,OffsetFlag = UT_OF_Open
	//ETF赎回:HedgeFlag = UT_HF_Redemption,Direction = UT_D_Sell,OffsetFlag = UT_OF_Close
	pInputOrderField->HedgeFlag = UT_HF_Speculation;
	pInputOrderField->Direction = UT_D_Buy;	
	pInputOrderField->OffsetFlag = UT_OF_Open;
	pInputOrderField->LimitPrice = 3.5;
	pInputOrderField->VolumeTotalOriginal = 100;
	pInputOrderField->TimeCondition = UT_TC_GFD;
	pInputOrderField->VolumeCondition = UT_VC_AV;
	pInputOrderField->MinVolume = 0;
	pInputOrderField->ContingentCondition = UT_CC_Immediately;
	pInputOrderField->StopPrice = 0;
	pInputOrderField->IsAutoSuspend = 0;
}

void gen_order_action(CUTInputOrderActionField* pInputOrderActionField, CUTOrderField* pOrder)
{
	memset(pInputOrderActionField, 0, sizeof(CUTInputOrderActionField));
	pInputOrderActionField->FrontID=pOrder->FrontID;
	pInputOrderActionField->SessionID = pOrder->SessionID;
	pInputOrderActionField->OrderRef=pOrder->OrderRef;
	
	pInputOrderActionField->OrderActionRef = ++g_reqid;
	pInputOrderActionField->ActionFlag = UT_AF_Delete;
}
	
class TradeSpi : public CUTSpi
{	
	public:
		TradeSpi(CUTApi* api)
		{
			m_api = api;
		}

		~TradeSpi(void)
		{
		}

	public:
		///当客户端与交易后台建立起通信连接时（还未登录前），该方法被调用。
		virtual void OnFrontConnected()
		{
			printf("OnFrontConnected!\n");
			
			//登录请求
			CUTReqLoginField reqLoginField;
			memset(&reqLoginField, 0, sizeof(reqLoginField));
			strcpy(reqLoginField.UserID, g_userid);
			strcpy(reqLoginField.Password, g_password);
			strcpy(reqLoginField.UserProductInfo, "xxx");			
			m_api->ReqLogin(&reqLoginField, 0);		
		}
	
		///当客户端与交易后台通信连接断开时，该方法被调用。当发生这个情况后，API会自动重新连接，客户端可不做处理。
		///@param nReason 错误原因
		///        0x1001 网络读失败
		///        0x1002 网络写失败
		///        0x2001 接收心跳超时
		///        0x2002 发送心跳失败
		///        0x2003 收到错误报文
		virtual void OnFrontDisconnected(int nReason)
		{
			printf("OnFrontDisconnected:%d\n", nReason);
		}

		///错误应答
		virtual void OnRspError(CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
			if (pRspInfo && pRspInfo->ErrorID)
			{
				printf("OnRspError: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
			}
		}

		///登录请求响应
		virtual void OnRspLogin(CUTRspLoginField *pRspUserLogin, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
			if (pRspInfo==NULL || pRspInfo->ErrorID == 0)
			{
				printf("OnRspLogin: OK!\n");

				// 合约查询
				CUTQryInstrumentField QryInstrument;
				memset(&QryInstrument, 0, sizeof(CUTQryInstrumentField));
				//strcpy(QryInstrument.InstrumentID, "510050");
				if(0 != m_api->ReqQryInstrument(&QryInstrument, ++g_reqid))
				{
					printf("ReqQryInstrument: Error!\n");
				}
			}		
			else
			{
				printf("OnRspLogin: Error! [%d] [%s]\n",pRspInfo->ErrorID, pRspInfo->ErrorMsg);
			}
		}

		///请求查询合约响应
		virtual void OnRspQryInstrument(CUTInstrumentField *pInstrument, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
		
			if (pInstrument != NULL)
			{
				//ProductID:股票--ASTOCK,基金--ETF,债券--BOND,指数--INDEX
				//printf("Instrument:[%s] [%d] [%s]\n", pInstrument->InstrumentID, pInstrument->ExchangeID, pInstrument->InstrumentName);
				
			}
				
			if (bIsLast)
			{
				if (pRspInfo && pRspInfo->ErrorID)
				{
					printf("OnRspQryInstrument: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
				}
				else
				{
					printf("OnRspQryInstrument:OK\n");
				}
				//查询合约结束，查询资金;必须等上一个查询结束后,才可进行下一个查询			
				CUTQryTradingAccountField QryTradingAccount;
				memset(&QryTradingAccount, 0, sizeof(CUTQryTradingAccountField));				
				if (0 != m_api->ReqQryTradingAccount(&QryTradingAccount, ++g_reqid))
				{
					printf("ReqQryTradingAccount: Error!\n");
				}
			}
		}

		///请求查询资金响应
		virtual void OnRspQryTradingAccount(CUTTradingAccountField *pTradingAccount, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
			if (pTradingAccount != NULL)
			{
				printf("TradingAccount:[%s] [%.2f]\n", pTradingAccount->AccountID, pTradingAccount->Available);
			}
			

			if (bIsLast)
			{
				if (pRspInfo && pRspInfo->ErrorID)
				{
					printf("OnRspQryTradingAccount: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
				}
				else
				{
					printf("OnRspQryTradingAccount:OK\n");
				}
				//查询资金结束，查询持仓				
				CUTQryInvestorPositionField QryInvestorPosition;
				memset(&QryInvestorPosition, 0, sizeof(CUTQryInvestorPositionField));				
				if (0 != m_api->ReqQryInvestorPosition(&QryInvestorPosition, ++g_reqid))
				{
					printf("ReqQryInvestorPosition: Error!\n");
				}
			}
		}

		///请求查询持仓响应
		virtual void OnRspQryInvestorPosition(CUTInvestorPositionField *pInvestorPosition, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
			if (pInvestorPosition != NULL)
			{
				if (0 == strcmp(pInvestorPosition->InstrumentID, "SHRQ88") || 0 == strcmp(pInvestorPosition->InstrumentID, "SZRQ88"))
				{
					printf("InvestorPosition（历史逆回购标准券,不可平):[%s] [%s] [%d]\n", pInvestorPosition->InvestorID, pInvestorPosition->InstrumentID, pInvestorPosition->Position);
				}
				else if (pInvestorPosition->PosiDirection == UT_PD_Short)
				{
					printf("InvestorPosition（今日逆回购持仓,不可平):[%s] [%s] [%d]\n", pInvestorPosition->InvestorID, pInvestorPosition->InstrumentID, pInvestorPosition->Position);
				}
				else if (pInvestorPosition->PosiDirection == UT_PD_Long)
				{
					printf("InvestorPosition（非逆回购持仓）:[%s] [%s] [%d]\n", pInvestorPosition->InvestorID, pInvestorPosition->InstrumentID, pInvestorPosition->Position);
				}
			}
			
			if (bIsLast)
			{
				if (pRspInfo && pRspInfo->ErrorID)
				{
					printf("OnRspQryInvestorPosition: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
				}
				else
				{
					printf("OnRspQryInvestorPosition:OK\n");
				}
				//查询持仓结束，查询报单				
				CUTQryOrderField QryOrder;
				memset(&QryOrder, 0, sizeof(CUTQryOrderField));
				if (0 != m_api->ReqQryOrder(&QryOrder, ++g_reqid))
				{
					printf("ReqQryOrder: Error!\n");
				}
			}
		}

		///请求查询报单响应
		virtual void OnRspQryOrder(CUTOrderField *pOrder, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
			if (pOrder != NULL)
			{
				printf("Order:[%s] [%s] [%d]\n", pOrder->InvestorID, pOrder->InstrumentID, pOrder->VolumeTotalOriginal);
			}

			if (bIsLast)
			{
				if (pRspInfo && pRspInfo->ErrorID)
				{
					printf("OnRspQryOrder: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
				}
				else
				{
					printf("OnRspQryOrder: OK\n");
				}
				//查询报单结束，查询成交				
				CUTQryTradeField QryTrade;
				memset(&QryTrade, 0, sizeof(CUTQryTradeField));				
				if (0 != m_api->ReqQryTrade(&QryTrade, ++g_reqid))
				{
					printf("ReqQryTrade: Error!\n");
				}
			}
		}

		///请求查询成交响应
		virtual void OnRspQryTrade(CUTTradeField *pTrade, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
			if (pTrade != NULL)
			{
				printf("Trade:[%s] [%s] [%d]\n", pTrade->InvestorID, pTrade->InstrumentID, pTrade->Volume);
			}

			if (bIsLast)
			{		
				if (pRspInfo && pRspInfo->ErrorID)
				{
					printf("OnRspQryTrade: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
				}
				else
				{
					printf("OnRspQryTrade: OK\n");
				}
				
				//查询可申购ETF信息(每手数量,现金替代比例等)
				CUTQryETFInfoField QryETFInfo;
				memset(&QryETFInfo, 0, sizeof(CUTQryETFInfoField));
				if (0 != m_api->ReqQryETFInfo(&QryETFInfo, ++g_reqid))
				{
					printf("ReqQryETFInfo: Error!\n");
				}
				//查询可申购ETF对应的成分股
				CUTQryETFComponentField QryETFComponent;
				memset(&QryETFComponent, 0, sizeof(CUTQryETFComponentField));
				if (0 != m_api->ReqQryETFComponent(&QryETFComponent, ++g_reqid))
				{
					printf("ReqQryETFComponent: Error!\n");
				}
			}
		}

		///请求查询ETF信息响应
		virtual void OnRspQryETFInfo(CUTETFInfoField *pETFInfo, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
			if (pETFInfo != NULL)
			{
				//printf("ETFInfo:[%c] [%s] [%d]\n", pETFInfo->ExchangeID, pETFInfo->InstrumentID, pETFInfo->CreationRedemptionUnit);
			}

			if (bIsLast)
			{
				if (pRspInfo && pRspInfo->ErrorID)
				{
					printf("OnRspQryETFInfo: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
				}
				else
				{
					printf("OnRspQryETFInfo: OK\n");
				}
			}
		}


		///请求查询ETF成分股响应
		virtual void OnRspQryETFComponent(CUTETFComponentField *pETFComponent, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
			if (pETFComponent != NULL)
			{
				//printf("ETFComponent:[%c] [%s] [%s] [%d]\n", pETFComponent->ExchangeID, pETFComponent->ETFID, pETFComponent->InstrumentID, pETFComponent->Volume);
			}

			if (bIsLast)
			{
				if (pRspInfo && pRspInfo->ErrorID)
				{
					printf("OnRspQryETFComponent: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
				}
				else
				{
					printf("OnRspQryETFComponent: OK\n");
				}

				//查询结束，开始下单
				CUTInputOrderField inputOrderField;
				for (int i = 0; i < 1; i++)
				{
					gen_order(&inputOrderField);
					m_api->ReqOrderInsert(&inputOrderField, g_reqid);
				}
			}
		}
		///报单录入请求错误时的响应;正确时不会产生该响应,而是回调OnRtnOrder
		virtual void OnRspOrderInsert(CUTInputOrderField *pInputOrder, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{	
			printf("OnRspOrderInsert: Error! [%d] [%d] [%s] [%s] [%s] [%d]\n", nRequestID, pRspInfo->ErrorID, pRspInfo->ErrorMsg, pInputOrder->InvestorID, pInputOrder->InstrumentID, pInputOrder->OrderRef);		
		}

		///报单通知
		virtual void OnRtnOrder(CUTOrderField *pOrder)
		{
			printf("OnRtnOrder:[%s] [%s] [%d]\n", pOrder->InvestorID, pOrder->InstrumentID, pOrder->OrderRef);
			//发一笔撤单;ETF申赎不可撤单
			/*if (pOrder->OrderStatus == UT_OST_Unknown)
			{
				CUTInputOrderActionField inputOrderActionField;
				gen_order_action(&inputOrderActionField, pOrder);
				m_api->ReqOrderAction(&inputOrderActionField, g_reqid);
			}*/
		}

		///报单操作错误，被UT打回时的响应;正确时不会产生该响应,而是回调OnRtnOrder
		virtual void OnRspOrderAction(CUTInputOrderActionField *pInputOrderAction, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast)
		{
			printf("OnRspOrderAction: Error! [%d] [%d] [%s] [%d]\n", nRequestID, pRspInfo->ErrorID, pRspInfo->ErrorMsg, pInputOrderAction->OrderRef);
		}

		///报单操作错误，被交易所打回时的回报
		virtual void OnErrRtnOrderAction(CUTOrderActionField *pOrderAction)
		{
			printf("OnErrRtnOrderAction: [%s] [%d]\n", pOrderAction->OrderRef, pOrderAction->ExchangeErrorID);
		}
	
		///成交通知
		virtual void OnRtnTrade(CUTTradeField *pTrade)
		{
			if (pTrade->HedgeFlag != UT_HF_Redemption)
			{
				//非ETF成交
				printf("OnRtnTrade:%s,%s,%s\n", pTrade->InvestorID, pTrade->InstrumentID, "非ETF申赎成交");	
			}
			else
			{
				//ETF成交
				printf("OnRtnTrade:%s,%s,%.2f,%s\n", pTrade->InvestorID, pTrade->InstrumentID, pTrade->Price,
					(pTrade->TradeType == UT_TRDT_ETFComponent ? "ETF申赎成分股成交" : pTrade->TradeType == UT_TRDT_ETFMoney ? "ETF申赎资金成交" : "ETF申赎成交"));
			}		
		}
		
	private:
		CUTApi* m_api;
};

int main(int argc, char *argv[])
{
	//创建api;将参数nCPUID设置为需要绑定的CPU,可开启极速模式
	//如果同一进程内创建多个api，参数pszFlowPath必须设置为不同的路径
	CUTApi* api = CUTApi::CreateApi();
	
	//创建并注册spi
	TradeSpi spi(api);
	api->RegisterSpi(&spi);
	
	// 注册前置地址	
	api->RegisterFront(g_front_address);

	//订阅私有流;这个函数也可以在登录成功后的任何地方调用
	api->SubscribePrivateTopic(UT_TERT_QUICK);
	//暂时没有公有流
	//api->SubscribePublicTopic(UT_TERT_QUICK);
	
	//启动api开始工作
	api->Init();

	//等待api线程的结束
	api->Join();
	//api释放
	//api->Release();
	
	return 0;
}
