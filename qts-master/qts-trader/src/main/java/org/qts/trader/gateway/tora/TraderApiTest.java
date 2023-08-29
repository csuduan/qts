package org.qts.trader.gateway.tora;

import java.io.IOException;
import com.tora.traderapi.*;


public class TraderApiTest {

	static
	{
		System.loadLibrary("javatraderapi");
	}

	public static void main(String[] args) throws IOException, InterruptedException
	{
		System.out.printf("Version[%s]\n", CTORATstpTraderApi.GetApiVersion());

		CTORATstpTraderApi api = CTORATstpTraderApi.CreateTstpTraderApi();
		DemoTdSpi spi = new DemoTdSpi(api);
		api.RegisterSpi(spi);
		api.RegisterFront("tcp://210.14.72.21:4400");

		api.SubscribePrivateTopic(TORA_TE_RESUME_TYPE.TORA_TERT_RESTART);
		api.SubscribePublicTopic(TORA_TE_RESUME_TYPE.TORA_TERT_RESTART);

		api.Init();

		System.in.read();
	}
}

class DemoTdSpi extends CTORATstpTraderSpi {
	String UserID = "admin";
	String Password = "123456";
	String InvestorID = "123456";
	String AccountID = "123456";
	String SSEShareholderID = "123456";
	String SZSEShareholderID = "123456";

	CTORATstpTraderApi m_api;
	int m_request_id;
	
	public DemoTdSpi(CTORATstpTraderApi api)
	{
		m_api = api;
		m_request_id = 0;
	}
	
	public void OnFrontConnected()
	{
		System.out.printf("OnFrontConnected\n");
		
		// ���ӳɹ������Ե�½
		CTORATstpReqUserLoginField req_user_login_field = new CTORATstpReqUserLoginField();
		
		req_user_login_field.setLogInAccount(UserID);
		req_user_login_field.setLogInAccountType(traderapi.getTORA_TSTP_LACT_UserID());
		req_user_login_field.setPassword(Password);
		req_user_login_field.setUserProductInfo("javademo");
		
		int ret = m_api.ReqUserLogin(req_user_login_field, ++m_request_id);
		if (ret != 0)
		{
			System.out.printf("ReqUserLogin fail, ret[%d]\n", ret);
		}
	}
	
	public void OnFrontDisconnected(int nReason)
	{
		System.out.printf("OnFrontDisconnected, reason[%d]\n", nReason);
	}

	public void OnRspUserLogin(CTORATstpRspUserLoginField pRspUserLoginField, CTORATstpRspInfoField pRspInfo, int nRequestID)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("login success!\n");
			/*
			if (true)
			{
				// ��ѯ��Լ��Ϣ
				CTORATstpQrySecurityField qry_security_field = new CTORATstpQrySecurityField();

				qry_security_field.setExchangeID(traderapi.getTORA_TSTP_EXD_SSE());
				qry_security_field.setSecurityID("600000");

				int ret = m_api.ReqQrySecurity(qry_security_field, ++m_request_id);
				if (ret != 0)
				{
					System.out.printf("ReqQrySecurity fail, ret[%d]\n", ret);
				}
			}

			if (true)
			{
				// ��ѯͶ������Ϣ
				CTORATstpQryInvestorField qry_investor_field = new CTORATstpQryInvestorField();
				
				qry_investor_field.setInvestorID(InvestorID);
				
				int ret = m_api.ReqQryInvestor(qry_investor_field, ++m_request_id);
				if (ret != 0)
				{
					System.out.printf("ReqQryInvestor fail, ret[%d]\n", ret);
				}
			}

			if (true)
			{
				// ��ѯ�ɶ��˺�
				CTORATstpQryShareholderAccountField qry_shareholder_account_field = new CTORATstpQryShareholderAccountField();

				//qry_shareholder_account_field.setExchangeID(traderapi.getTORA_TSTP_EXD_SSE());
				qry_shareholder_account_field.setInvestorID(InvestorID);
				
				int ret = m_api.ReqQryShareholderAccount(qry_shareholder_account_field, ++m_request_id);
				if (ret != 0)
				{
					System.out.printf("ReqQryShareholderAccount fail, ret[%d]\n", ret);
				}
			}


			if (true)
			{
				// ��ѯ�ʽ��˺�
				CTORATstpQryTradingAccountField qry_trading_account_field = new CTORATstpQryTradingAccountField();
				
				qry_trading_account_field.setInvestorID(InvestorID);
				qry_trading_account_field.setAccountID(AccountID);
				
				int ret = m_api.ReqQryTradingAccount(qry_trading_account_field, ++m_request_id);
				if (ret != 0)
				{
					System.out.printf("ReqQryTradingAccount fail, ret[%d]\n", ret);
				}
			}

			if (true)
			{
				// ��ѯ�ֲ�
				CTORATstpQryPositionField qry_position_field = new CTORATstpQryPositionField();
				
				qry_position_field.setInvestorID(InvestorID);
				//qry_position_field.setSecurityID("600000");

				int ret = m_api.ReqQryPosition(qry_position_field, ++m_request_id);
				if (ret != 0)
				{
					System.out.printf("ReqQryPosition fail, ret[%d]\n", ret);
				}
			}

			if (true)
			{
				// ��ѯ����
				CTORATstpQryOrderField qry_order_field = new CTORATstpQryOrderField();
				
				//qry_order_field.setSecurityID("600000");
				//qry_order_field.setInsertTimeStart("09:35:00");
				//qry_order_field.setInsertTimeEnd("10:30:00");
				
				int ret = m_api.ReqQryOrder(qry_order_field, ++m_request_id);
				if (ret != 0)
				{
					System.out.printf("ReqQryOrder fail, ret[%d]\n", ret);
				}
			}
			*/
			if (true)
			{
				// ���󱨵�
				CTORATstpInputOrderField input_order_field = new CTORATstpInputOrderField();

				input_order_field.setExchangeID(traderapi.getTORA_TSTP_EXD_SSE());
				input_order_field.setSecurityID("600000");
				input_order_field.setShareholderID(SSEShareholderID);
				input_order_field.setDirection(traderapi.getTORA_TSTP_D_Buy());
				input_order_field.setVolumeTotalOriginal(100);
				input_order_field.setLimitPrice(3700);
				input_order_field.setOrderPriceType(traderapi.getTORA_TSTP_OPT_LimitPrice());
				input_order_field.setTimeCondition(traderapi.getTORA_TSTP_TC_GFD());
				input_order_field.setVolumeCondition(traderapi.getTORA_TSTP_VC_AV());

				int ret = m_api.ReqOrderInsert(input_order_field, ++m_request_id);
				if (ret != 0)
				{
					System.out.printf("ReqOrderInsert fail, ret[%d]\n", ret);
				}
			}
			/*
			if (true)
			{
				// ���󳷵�
				CTORATstpInputOrderActionField input_order_action_field = new CTORATstpInputOrderActionField();

				input_order_action_field.setExchangeID(traderapi.getTORA_TSTP_EXD_SSE());
				input_order_action_field.setActionFlag(traderapi.getTORA_TSTP_AF_Delete());
				input_order_action_field.setOrderSysID("110010010000002");

				int ret = m_api.ReqOrderAction(input_order_action_field, ++m_request_id);
				if (ret != 0)
				{
					System.out.printf("ReqOrderAction fail, ret[%d]\n", ret);
				}
			}
			*/
		}
		else 
		{
			System.out.printf("login fail, error_id[%d], error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspQrySecurity(CTORATstpSecurityField pSecurity, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pSecurity != null)
		{
			System.out.printf("OnRspQrySecurity[%d]: SecurityID[%s] SecurityName[%s] UpperLimitPrice[%.3f] LowerLimitPrice[%.3f]\n",
					nRequestID, pSecurity.getSecurityID(), pSecurity.getSecurityName(),
					pSecurity.getUpperLimitPrice(), pSecurity.getLowerLimitPrice());
		}
		
		if (bIsLast)
		{
			System.out.printf("��ѯ��Լ��Ϣ����!\n");
		}
	}
	
	public void OnRspQryInvestor(CTORATstpInvestorField pInvestor, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pInvestor != null)
		{
			System.out.printf("OnRspQryInvestor[%d]: InvestorID[%s] InvestorName[%s] Operways[%s]\n",
					nRequestID, pInvestor.getInvestorID(), pInvestor.getInvestorName(),
					pInvestor.getOperways());
		}
		
		if (bIsLast)
		{
			System.out.printf("��ѯͶ������Ϣ����!\n");
		}
	}
	
	public void OnRspQryShareholderAccount(CTORATstpShareholderAccountField pShareholderAccount, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pShareholderAccount != null)
		{
			System.out.printf("OnRspQryShareholderAccount[%d]: InvestorID[%s] ExchangeID[%c] MarketID[%c] ShareholderID[%s]\n",
					nRequestID, pShareholderAccount.getInvestorID(), pShareholderAccount.getExchangeID(),
					pShareholderAccount.getMarketID(), pShareholderAccount.getShareholderID());
		}

		if (bIsLast)
		{
			System.out.printf("��ѯ�ɶ��˺Ž���!\n");
		}
	}
	
	public void OnRspQryTradingAccount(CTORATstpTradingAccountField pTradingAccount, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pTradingAccount != null)
		{
			System.out.printf("OnRspQryTradingAccount[%d]: DepartmentID[%s] InvestorID[%s] AccountID[%s] CurrencyID[%c] UsefulMoney[%.2f] WithdrawQuota[%.2f]\n",
					nRequestID, pTradingAccount.getDepartmentID(), pTradingAccount.getInvestorID(),
					pTradingAccount.getAccountID(), pTradingAccount.getCurrencyID(), pTradingAccount.getUsefulMoney(),
					pTradingAccount.getWithdraw());
		}
		
		if (bIsLast)
		{
			System.out.printf("��ѯ�ʽ��˺Ž���!\n");
		}
	}
	
	public void OnRspQryPosition(CTORATstpPositionField pPosition, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pPosition != null)
		{
			System.out.printf("OnRspQryPosition[%d]: InvestorID[%s] SecurityID[%s] HistoryPos[%d] TodayBSPos[%d] TodayPRPos[%d]\n", 
					nRequestID, pPosition.getInvestorID(), pPosition.getSecurityID(),
					pPosition.getHistoryPos(), pPosition.getTodayBSPos(), pPosition.getTodayPRPos());
		}
		
		if (bIsLast)
		{
			System.out.printf("��ѯ�ֲֽ���!\n");
		}
	}
	
	public void OnRspQryOrder(CTORATstpOrderField pOrder, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pOrder != null)
		{
			System.out.printf("OnRspQryOrder[%d]: SecurityID[%s] OrderLocalID[%s] OrderRef[%s] OrderSysID[%s] VolumeTraded[%d] OrderStatus[%c] OrderSubmitStatus[%c] StatusMsg[%s]\n",
					nRequestID, pOrder.getSecurityID(), pOrder.getOrderLocalID(), pOrder.getOrderRef(),
					pOrder.getOrderSysID(), pOrder.getVolumeTraded(), pOrder.getOrderStatus(), 
					pOrder.getOrderSubmitStatus(), pOrder.getStatusMsg());
		}
		
		if (bIsLast)
		{
			System.out.printf("��ѯ��������!\n");
		}
	}

	public void OnRspOrderInsert(CTORATstpInputOrderField pInputOrderField, CTORATstpRspInfoField pRspInfo, int nRequestID)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspOrderInsert: OK! [%d]\n",	nRequestID);
		}
		else
		{
			System.out.printf("OnRspOrderInsert: Error! [%d] [%d] [%s]\n", nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRtnOrder(CTORATstpOrderField pOrder)
	{
		System.out.printf("OnRtnOrder: InvestorID[%s] SecurityID[%s] OrderRef[%s] OrderLocalID[%s] LimitPrice[%.2f] VolumeTotalOriginal[%d] OrderSysID[%s] OrderStatus[%c]\n", 
				pOrder.getInvestorID(), pOrder.getSecurityID(), pOrder.getOrderRef(), pOrder.getOrderLocalID(),
				pOrder.getLimitPrice(), pOrder.getVolumeTotalOriginal(), pOrder.getOrderSysID(),
				pOrder.getOrderStatus());
	}
	
	public void OnRtnTrade(CTORATstpTradeField pTrade)
	{
		System.out.printf("OnRtnTrade: TradeID[%s] InvestorID[%s] SecurityID[%s] OrderRef[%s] OrderLocalID[%s] Price[%.2f] Volume[%d]\n",
				pTrade.getTradeID(), pTrade.getInvestorID(), pTrade.getSecurityID(), 
				pTrade.getOrderRef(), pTrade.getOrderLocalID(), pTrade.getPrice(), pTrade.getVolume());
	}

	public void OnRspOrderAction(CTORATstpInputOrderActionField pInputOrderActionField, CTORATstpRspInfoField pRspInfo, int nRequestID)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspOrderAction: OK! [%d]\n", nRequestID);
		}
		else
		{
			System.out.printf("OnRspOrderAction: Error! [%d] [%d] [%s]\n", nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}

	public void OnRspTransferFund(CTORATstpInputTransferFundField pInputTransferFundField, CTORATstpRspInfoField pRspInfo, int nRequestID)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspTransferFund: OK! [%d]\n", nRequestID);
		}
		else
		{
			System.out.printf("OnRspTransferFund: Error! [%d] [%d] [%s]\n", nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
}




