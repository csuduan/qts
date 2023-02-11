package com.bingbei.mts.trade.gateway.ost;

import ut.api.*;


class TraderSpiImpl extends CUTSpi{
	final static String m_UserId = "00301";
	final static String m_InvestorId = "00301";
	final static String m_PassWord = "111111";
//	final static String m_PassWord = "1";

	final static String m_CurrencyId = "CNY";
	static int m_maxOrderRef = 0;
	static int m_requestID = 1;

	TraderSpiImpl(CUTApi traderapi)
	{
		m_traderapi =  traderapi;
	}

	@Override
	public void OnFrontConnected(){

		System.out.println("On Front Connected");
		System.out.println("Connected 当前执行的线程Id是:"+Thread.currentThread().getId()+"\n");
		CUTReqLoginField field = new CUTReqLoginField();
		field.setUserID(m_UserId);
		field.setPassword(m_PassWord);
		field.setUserProductInfo("JAVA_API");
		m_traderapi.ReqLogin(field,0);
		System.out.println("Send login ok");
	}
	@Override
	public void OnFrontDisconnected(int nReason){

		System.out.println("OnFrontDisconnected 当前执行的线程Id是:"+Thread.currentThread().getId()+"\n");

	}

	@Override
	public void OnRspLogin(CUTRspLoginField pRspUserLogin, CUTRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		System.out.println("OnRspLogin 当前执行的线程Id是:"+Thread.currentThread().getId()+"\n");
		if (pRspInfo != null && pRspInfo.getErrorID() != 0)
		{
			System.out.printf("Login ErrorID[%d] ErrMsg1[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());

			return;
		}
		System.out.println("Login success!!!");

		///////////////////////
		CUTQryTradingAccountField qry = new CUTQryTradingAccountField();
		qry.setCurrencyID(m_CurrencyId);
		qry.setInvestorID(m_InvestorId);
		m_traderapi.ReqQryTradingAccount(qry, ++m_requestID);

		/////////////////
//		CUTQryInstrumentField qry = new CUTQryInstrumentField();
//		m_traderapi.ReqQryInstrument(qry, ++m_requestID);

		//////////////////
//		CUTQryInvestorField  qry = new CUTQryInvestorField();
//		m_traderapi.ReqQryInvestor(qry, ++m_requestID);
		//////////////////////
	}

	@Override
	public void OnRspQryTradingAccount(CUTTradingAccountField pTradingAccount, CUTRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		System.out.println("OnRspQryTradingAccount 当前执行的线程Id是:"+Thread.currentThread().getId()+"\n");
		if (pRspInfo != null && pRspInfo.getErrorID() != 0)
		{
			System.out.printf("OnRspQryTradingAccount ErrorID[%d] ErrMsg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());

			return;
		}

		if (pTradingAccount != null)
		{
			System.out.printf("Balance[%f]Available[%f]WithdrawQuota[%f]Credit[%f]\n",
					pTradingAccount.getBalance(), pTradingAccount.getAvailable(), pTradingAccount.getWithdrawQuota(),
					pTradingAccount.getCredit());
		}
		else
		{
			System.out.printf("NULL obj\n");
		}

//		//////////////////
//		CUTInputOrderField inputOrderField = new CUTInputOrderField();
//		inputOrderField.setInvestorID(m_InvestorId);
//		inputOrderField.setInstrumentID("000001");
//		inputOrderField.setExchangeID(apiConstants.UT_EXG_SZSE);
//
//		//OrderRef必须设置,同一会话内必须递增,可以不连续
//		inputOrderField.setOrderRef(++m_maxOrderRef);
//		inputOrderField.setOrderPriceType(apiConstants.UT_OPT_LimitPrice);
//		inputOrderField.setDirection(apiConstants.UT_D_Buy);
//		inputOrderField.setOffsetFlag(apiConstants.UT_OF_Open);
//		inputOrderField.setHedgeFlag(apiConstants.UT_HF_Speculation);
//		inputOrderField.setLimitPrice(88.6);
//		inputOrderField.setVolumeTotalOriginal(200);
//
//		inputOrderField.setTimeCondition(apiConstants.UT_TC_GFD);
//		inputOrderField.setVolumeCondition(apiConstants.UT_VC_AV);
//		inputOrderField.setMinVolume(0);
//
//		int nRet = m_traderapi.ReqOrderInsert(inputOrderField ,++m_requestID);
//		System.out.printf("ReqOrderInsert nRet=[%d] !\n", nRet);

		////////////////////////////
		CUTQryOrderField req = new CUTQryOrderField();
		req.setInvestorID(m_InvestorId);
		m_traderapi.ReqQryOrder(req, ++m_requestID);
	}


	@Override
	public void OnRspQryInstrument(CUTInstrumentField pInstrument, CUTRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		//System.out.println("OnRspQryTradingAccount 当前执行的线程Id是:"+Thread.currentThread().getId()+"\n");
		if (pInstrument != null)
		{
			System.out.printf("pInstrument Name[%s]\n", pInstrument.getInstrumentName());
		}
	}

	///请求查询投资者响应
	@Override
	public void OnRspQryInvestor(CUTInvestorField pInvestor, CUTRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pInvestor != null)
		{
			System.out.printf("pInvestor [%s]\n", pInvestor.getInvestorName());
		}
	}

	@Override
	public void OnRtnOrder(CUTOrderField pOrder)
	{
		if(pOrder != null)
		{
			System.out.printf("OnRtnOrder InstrmentId=[%s] OrderRef[%d] OrderStatus=[%c]!\n",
					pOrder.getInstrumentID(),
					pOrder.getOrderRef(),
					pOrder.getOrderStatus());
		}
	}

	@Override
	public void OnRtnTrade(CUTTradeField pTrade)
	{

		if(pTrade != null)
		{
			System.out.printf("OnRtnTrade InstrmentId=[%s] OrderRef[%d] Price=[%f]!\n",
					pTrade.getInstrumentID(),
					pTrade.getOrderRef(),
					pTrade.getPrice());
		}
	}

	///报单错误回报
	@Override
	public void OnRspOrderInsert(CUTInputOrderField pInputOrder, CUTRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		System.out.printf("OnRspOrderInsert !\n");
		if(pRspInfo != null)
		{

		}
	}

	///请求查询报单响应
	@Override
	public void OnRspQryOrder(CUTOrderField pInputOrder, CUTRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		System.out.println("OnRspQryOrder 当前执行的线程Id是:"+Thread.currentThread().getId()+"\n");

		//System.out.println("OnRspQryOrder this object:"+ this+ "\n");
		if(pRspInfo != null)
		{

		}
	}
	public void helloFromJava() {
		System.out.println("helloFromJava 当前执行的线程Id是:"+Thread.currentThread().getName()+"\n");
		System.out.println("helloFromJava 888");
	}

	///////////////
	private CUTApi m_traderapi;
}

public class tradeapidemo{

	static{
		System.out.println(System.getProperty("java.library.path"));
		System.loadLibrary("utapi");
		System.loadLibrary("utapi_wrap");
	}
	final static String ctp1_TradeAddress = "tcp://192.168.35.202:8888";
	public static void main(String[] args) {
		// TODO Auto-generated method stub


		CUTApi traderApi = CUTApi.CreateApi();
		TraderSpiImpl pTraderSpi = new TraderSpiImpl(traderApi);
		traderApi.RegisterSpi(pTraderSpi);
		traderApi.RegisterFront(ctp1_TradeAddress);
		traderApi.SubscribePublicTopic(UT_TE_RESUME_TYPE.UT_TERT_QUICK);
		traderApi.SubscribePrivateTopic(UT_TE_RESUME_TYPE.UT_TERT_QUICK);
		traderApi.Init();
		traderApi.Join();

		return;
	}
}
