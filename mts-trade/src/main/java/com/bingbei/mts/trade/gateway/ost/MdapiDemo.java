package com.bingbei.mts.trade.gateway.ost;

import ut.api.*;

class MdspiImpl extends CUTMDSpi{
	final static String m_UserId = "00301";
	final static String m_InvestorId = "00301";
	final static String m_PassWord = "111111";

	final static String m_CurrencyId = "CNY";
	MdspiImpl(CUTMDApi mdapi)
	{
		m_mdapi =  mdapi;
	}

	public void OnFrontConnected(){
		System.out.println("On Front Connected");
		CUTReqLoginField field = new CUTReqLoginField();

		field.setUserID(m_UserId);
		field.setPassword(m_PassWord);
		m_mdapi.ReqLogin(field, 0);

	}

	public void OnRspLogin(CUTRspLoginField pRspUserLogin, CUTRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspUserLogin != null) {
			System.out.printf("UserID[%s]\n",pRspUserLogin.getUserID());
		}

		CUTSubInstrumentField field = new CUTSubInstrumentField();
		field.setExchangeID(apiConstants.UT_EXG_SZSE);
		field.setInstrumentID("000001");

		m_mdapi.SubscribeDepthMarketData(field,1);
	}

	public void OnRtnDepthMarketData(CUTDepthMarketDataField pDepthMarketData) {
		if (pDepthMarketData != null)
		{
			System.out.printf("InstrmentId[%s] LastPrice[%f]\n",
					pDepthMarketData.getInstrumentID(),
					pDepthMarketData.getLastPrice());
		}
		else
		{
			System.out.printf("NULL obj\n");
		}
	}
	private CUTMDApi m_mdapi;
}

public class MdapiDemo {
	static{
		System.loadLibrary("utmdapi");
		System.loadLibrary("utapi_wrap");
	}
	final static String ctp1_MdAddress = "tcp://192.168.35.202:33008";
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CUTMDApi mdApi = CUTMDApi.CreateMDApi();
		MdspiImpl pMdspiImpl = new MdspiImpl(mdApi);
		mdApi.RegisterSpi(pMdspiImpl);
		mdApi.RegisterFront(ctp1_MdAddress);
		mdApi.Init();
		mdApi.Join();
		return;
	}
}
