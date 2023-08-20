package org.qts.trader.gateway.tora; /**
 * �ļ���    :MdApiTest.java
 * ����ʱ��:2020��12��25������1:23:55
 * �汾��    :
 * ����	 ��xmdapitest
 * ����	 ��86158
 */
//package xmdapitest;

import java.io.IOException;
import com.tora.xmdapi.*;


public class XMdApiTest {

	static
	{
		System.loadLibrary("javaxmdapi");
	}

	public static void main(String[] args) throws IOException, InterruptedException
	{
		System.out.printf("Version[%s]\n", CTORATstpXMdApi.GetApiVersion());

		CTORATstpXMdApi api = CTORATstpXMdApi.CreateTstpXMdApi();
		DemoSpi spi = new DemoSpi(api);
		api.RegisterSpi(spi);
		api.RegisterFront("tcp://100.100.10.1:33402");
		// ������������ʱ�����ע����������
		api.RegisterDeriveServer("tcp://10.100.69.2:7401");

		api.Init();

		System.in.read();
	}
}
class DemoSpi extends CTORATstpXMdSpi
{
	CTORATstpXMdApi m_api;
	int m_request_id;
	
	public DemoSpi(CTORATstpXMdApi api)
	{
		m_api = api;
		m_request_id = 0;
	}
	
	public void OnFrontConnected()
	{
		System.out.printf("OnFrontConnected\n");
	
		// ���ӳɹ������Ե�½
		CTORATstpReqUserLoginField req_user_login_field = new CTORATstpReqUserLoginField();
				
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
			
			String arr[] = {"000001"};
			int ret = m_api.SubscribeMarketData(arr, xmdapi.getTORA_TSTP_EXD_SZSE());
			if (ret != 0)
			{
				System.out.printf("SubscribeMarketData fail, ret[%d]\n", ret);
			}
			
			String arr2[] = {"300760"};
			ret = m_api.SubscribeSimplifyMarketData(arr2, xmdapi.getTORA_TSTP_EXD_SZSE());
			if (ret != 0)
			{
				System.out.printf("SubscribeSimplifyMarketData fail, ret[%d]\n", ret);
			}
			
			String arr3[] = {"600004"};
			ret = m_api.SubscribeSecurityStatus(arr3, xmdapi.getTORA_TSTP_EXD_SSE());
			if (ret != 0)
			{
				System.out.printf("SubscribeSecurityStatus fail, ret[%d]\n", ret);
			}
			
			ret = m_api.SubscribeMarketStatus(xmdapi.getTORA_TSTP_EXD_SSE());
			if (ret != 0)
			{
				System.out.printf("SubscribeMarketStatus fail, ret[%d]\n", ret);
			}
			
			CTORATstpInquiryMarketDataField a = new CTORATstpInquiryMarketDataField();
			a.setExchangeID(xmdapi.getTORA_TSTP_EXD_SZSE());
			a.setSecurityID("000002");
			ret = m_api.ReqInquiryMarketDataMirror(a,++m_request_id);
			if (ret != 0)
			{
				System.out.printf("ReqInquiryMarketDataMirror fail, ret[%d]\n", ret);
			}
			
			String arr5[] = {"688002"};
			ret = m_api.SubscribePHMarketData(arr5, xmdapi.getTORA_TSTP_EXD_SSE());
			if (ret != 0)
			{
				System.out.printf("SubscribePHMarketData fail, ret[%d]\n", ret);
			}
			
			String arr4[] = {"000002"};
			ret = m_api.SubscribeSpecialMarketData(arr4, xmdapi.getTORA_TSTP_EXD_SZSE());
			if (ret != 0)
			{
				System.out.printf("SubscribeSpecialMarketData fail, ret[%d]\n", ret);
			}
			
// 			����Ϊ��������
			String arr6[] = {"600036"};
			ret = m_api.SubscribeRapidMarketData(arr6, xmdapi.getTORA_TSTP_EXD_SSE());
			if (ret != 0)
			{
				System.out.printf("SubscribeRapidMarketData fail, ret[%d]\n", ret);
			}
			
//			ret = m_api.SubscribeFundsFlowMarketData(arr6, xmdapi.getTORA_TSTP_EXD_SSE());
//			if (ret != 0)
//			{
//				System.out.printf("SubscribeFundsFlowMarketData fail, ret[%d]\n", ret);
//			}
			
			CTORATstpInquiryMarketDataField b = new CTORATstpInquiryMarketDataField();
			b.setExchangeID(xmdapi.getTORA_TSTP_EXD_SSE());
			b.setSecurityID("688002");
			ret = m_api.ReqInquiryPHMarketDataMirror(b,++m_request_id);
			if (ret != 0)
			{
				System.out.printf("ReqInquiryPHMarketDataMirror fail, ret[%d]\n", ret);
			}
			
			CTORATstpInquirySpecialMarketDataField c = new CTORATstpInquirySpecialMarketDataField();
			c.setExchangeID(xmdapi.getTORA_TSTP_EXD_SZSE());
			c.setSecurityID("000002");
			ret = m_api.ReqInquirySpecialMarketDataMirror(c,++m_request_id);
			if (ret != 0)
			{
				System.out.printf("ReqInquirySpecialMarketDataMirror fail, ret[%d]\n", ret);
			}
			
			ret = m_api.UnSubscribeMarketData(arr, xmdapi.getTORA_TSTP_EXD_SZSE());
			if (ret != 0)
			{
				System.out.printf("UnSubscribeMarketData fail, ret[%d]\n", ret);
			}
			
			ret = m_api.UnSubscribeSimplifyMarketData(arr2, xmdapi.getTORA_TSTP_EXD_SZSE());
			if (ret != 0)
			{
				System.out.printf("UnSubscribeSimplifyMarketData fail, ret[%d]\n", ret);
			}
			
			ret = m_api.UnSubscribeSecurityStatus(arr3, xmdapi.getTORA_TSTP_EXD_SZSE());
			if (ret != 0)
			{
				System.out.printf("UnSubscribeSecurityStatus fail, ret[%d]\n", ret);
			}
			
			ret = m_api.UnSubscribeMarketStatus(xmdapi.getTORA_TSTP_EXD_SSE());
			if (ret != 0)
			{
				System.out.printf("UnSubscribeMarketStatus fail, ret[%d]\n", ret);
			}
			
			ret = m_api.UnSubscribePHMarketData(arr5, xmdapi.getTORA_TSTP_EXD_SSE());
			if (ret != 0)
			{
				System.out.printf("UnSubscribePHMarketData fail, ret[%d]\n", ret);
			}
			
			ret = m_api.UnSubscribeSpecialMarketData(arr4, xmdapi.getTORA_TSTP_EXD_SZSE());
			if (ret != 0)
			{
				System.out.printf("UnSubscribeSpecialMarketData fail, ret[%d]\n", ret);
			}
			
			ret = m_api.UnSubscribeRapidMarketData(arr6, xmdapi.getTORA_TSTP_EXD_SSE());
			if (ret != 0)
			{
				System.out.printf("UnSubscribeRapidMarketData fail, ret[%d]\n", ret);
			}
			
//			ret = m_api.UnSubscribeFundsFlowMarketData(arr6, xmdapi.getTORA_TSTP_EXD_SSE());
//			if (ret != 0)
//			{
//				System.out.printf("UnSubscribeFundsFlowMarketData fail, ret[%d]\n", ret);
//			}
			
		}
		else
		{
			System.out.printf("login fail, error_id[%d], error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspSubMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspSubMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspSubMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspUnSubMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspUnSubMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspUnSubMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspSubSimplifyMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspSubSimplifyMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspSubSimplifyMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspUnSubSimplifyMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspUnSubSimplifyMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspUnSubSimplifyMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspSubSecurityStatus(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspSubSecurityStatus success!\n");
		}
		else
		{
			System.out.printf("OnRspSubSecurityStatus fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspSubMarketStatus(CTORATstpSpecificMarketField pSpecificMarketField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspSubMarketStatus success!\n");
		}
		else
		{
			System.out.printf("OnRspSubMarketStatus fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspUnSubMarketStatus(CTORATstpSpecificMarketField pSpecificMarketField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspUnSubMarketStatus success!\n");
		}
		else
		{
			System.out.printf("OnRspUnSubMarketStatus fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspUnSubSecurityStatus(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspUnSubSecurityStatus success!\n");
		}
		else
		{
			System.out.printf("OnRspUnSubSecurityStatus fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspInquiryMarketDataMirror(CTORATstpMarketDataField pMarketDataField, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspInquiryMarketDataMirror success!\n");
		}
		else
		{
			System.out.printf("OnRspInquiryMarketDataMirror fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspInquiryPHMarketDataMirror(CTORATstpPHMarketDataField pPHMarketDataField, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspInquiryPHMarketDataMirror success!\n");
		}
		else
		{
			System.out.printf("OnRspInquiryPHMarketDataMirror fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspInquirySpecialMarketDataMirror(CTORATstpSpecialMarketDataField pMarketDataField, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspInquirySpecialMarketDataMirror success!\n");
		}
		else
		{
			System.out.printf("OnRspInquirySpecialMarketDataMirror fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspSubPHMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspSubPHMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspSubPHMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspSubSpecialMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspSubSpecialMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspSubSpecialMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspUnSubPHMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspUnSubPHMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspUnSubPHMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspUnSubSpecialMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspUnSubSpecialMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspUnSubSpecialMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	

	public void OnRspSubRapidMarketData(CTORATstpSpecificSecurityField pSpecificSecurity, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspSubRapidMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspSubRapidMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspUnSubRapidMarketData(CTORATstpSpecificSecurityField pSpecificSecurity, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspUnSubRapidMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspUnSubRapidMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspSubFundsFlowMarketData(CTORATstpSpecificSecurityField pSpecificSecurity, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspSubFundsFlowMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspSubFundsFlowMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspUnSubFundsFlowMarketData(CTORATstpSpecificSecurityField pSpecificSecurity, CTORATstpRspInfoField pRspInfo)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspUnSubFundsFlowMarketData success!\n");
		}
		else
		{
			System.out.printf("OnRspUnSubFundsFlowMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	public void OnRspUserLogout(CTORATstpUserLogoutField pUserLogoutField, CTORATstpRspInfoField pRspInfo, int nRequestID)
	{
		if (pRspInfo.getErrorID() == 0)
		{
			System.out.printf("OnRspUserLogout success!\n");
		}
		else
		{
			System.out.printf("OnRspUserLogout fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}
	
	
	public void OnRtnMarketData(CTORATstpMarketDataField pMarketDataField)
	{
		System.out.printf("OnRtnMarketData: ExchangeID[%c] SecurityID[%s] SecurityName[%s] UpperLimitPrice[%.3f] LowerLimitPrice[%.3f] LastPrice[%.3f] AskPrice1[%.3f] AskVolume1[%d] BidPrice1[%.3f] BidVolume1[%d] UpdateTime[%s]\n",
				pMarketDataField.getExchangeID(), pMarketDataField.getSecurityID(), pMarketDataField.getSecurityName(),
				pMarketDataField.getUpperLimitPrice(), pMarketDataField.getLowerLimitPrice(), pMarketDataField.getLastPrice(),
				pMarketDataField.getAskPrice1(), pMarketDataField.getAskVolume1(),
				pMarketDataField.getBidPrice1(), pMarketDataField.getBidVolume1(),
				pMarketDataField.getUpdateTime());
	}
	
	public void OnRtnSimplifyMarketData(CTORATstpSimplifyMarketDataField pSimplifyMarketDataField)
	{
		System.out.printf("OnRtnSimplifyMarketData: ExchangeID[%c] SecurityID[%s] SecurityName[%s] UpperLimitPrice[%.3f] LowerLimitPrice[%.3f] LastPrice[%.3f] AskPrice1[%.3f] BidPrice1[%.3f] UpdateTime[%s]\n",
				pSimplifyMarketDataField.getExchangeID(), pSimplifyMarketDataField.getSecurityID(), pSimplifyMarketDataField.getSecurityName(),
				pSimplifyMarketDataField.getUpperLimitPrice(), pSimplifyMarketDataField.getLowerLimitPrice(), pSimplifyMarketDataField.getLastPrice(),
				pSimplifyMarketDataField.getAskPrice1(),
				pSimplifyMarketDataField.getBidPrice1(),
				pSimplifyMarketDataField.getUpdateTime());
	}
	
	public void OnRtnSecurityStatus(CTORATstpSecurityStatusField pSecurityStatusField)
	{
		System.out.printf("OnRtnSecurityStatus: ExchangeID[%c] SecurityID[%s] IsSupportMarginBuy[%d] IsSupportShortSell[%d]\n",
				pSecurityStatusField.getExchangeID(), pSecurityStatusField.getSecurityID(), pSecurityStatusField.getIsSupportMarginBuy(),
				pSecurityStatusField.getIsSupportShortSell());
	}
	
	public void OnRtnMarketStatus(CTORATstpMarketStatusField pMarketStatusField)
	{
		System.out.printf("OnRtnMarketStatus: MarketID[%c] MarketStatus[%c]\n",
				pMarketStatusField.getMarketID(), pMarketStatusField.getMarketStatus());
	}
	
	public void OnRtnPHMarketData(CTORATstpPHMarketDataField pPHMarketDataField)
	{
		System.out.printf("OnRtnPHMarketData: ExchangeID[%c] SecurityID[%s] SecurityName[%s] UpperLimitPrice[%.3f] UpdateTime[%s]\n",
				pPHMarketDataField.getExchangeID(), pPHMarketDataField.getSecurityID(), pPHMarketDataField.getSecurityName(),
				pPHMarketDataField.getUpperLimitPrice(),
				pPHMarketDataField.getUpdateTime());
	}
	
	public void OnRtnSpecialMarketData(CTORATstpSpecialMarketDataField pSpecialMarketDataField)
	{
		System.out.printf("OnRtnSpecialMarketData: ExchangeID[%c] SecurityID[%s] SecurityName[%s] MovingAvgPrice[%.3f] UpdateTime[%s]\n",
				pSpecialMarketDataField.getExchangeID(), pSpecialMarketDataField.getSecurityID(), pSpecialMarketDataField.getSecurityName(),
				pSpecialMarketDataField.getMovingAvgPrice(),
				pSpecialMarketDataField.getUpdateTime());
	}
	
	public void OnRtnRapidMarketData(CTORATstpRapidMarketDataField pRapidMarketDataField)
	{
		System.out.printf("OnRtnRapidMarketData: [ExchangeID:%c][SecurityID:%s][PreClosePrice:$%.3f|OpenPrice:%.3f][NumTrades:$%d|TotalVolumeTrade:%d][TotalValueTrade:%.3f][HighestPrice:%.3f][LowestPrice:%.3f][LastPrice:%.3f][UpperLimitPrice:%.3f][LowerLimitPrice:%.3f]\n",
				pRapidMarketDataField.getExchangeID(),
				pRapidMarketDataField.getSecurityID(),
				pRapidMarketDataField.getPreClosePrice(),
				pRapidMarketDataField.getOpenPrice(),
				pRapidMarketDataField.getNumTrades(),
				pRapidMarketDataField.getTotalVolumeTrade(),
				pRapidMarketDataField.getTotalValueTrade(),
				pRapidMarketDataField.getHighestPrice(),
				pRapidMarketDataField.getLowestPrice(),
				pRapidMarketDataField.getLastPrice(),
				pRapidMarketDataField.getUpperLimitPrice(),
				pRapidMarketDataField.getLowerLimitPrice());
	}
	
//	public void OnRtnFundsFlowMarketData(CTORATstpFundsFlowMarketDataField pFundsFlowMarketData)
//	{
//		System.out.printf("OnRtnFundsFlowMarketData: [ExchangeID:%c][SecurityID:%s][UpdateTime:%s][B1$%.3f|%d|cnt:%d][S1$%.3f|%d|cnt:%d][B2$%.3f|%d|cnt:%d][S2$%.3f|%d|cnt:%d][B3$%.3f|%d|cnt:%d][S3$%.3f|%d|cnt:%d][B4$%.3f|%d|cnt:%d][S4$%.3f|%d|cnt:%d]\n",
//				pFundsFlowMarketData.getExchangeID(),
//				pFundsFlowMarketData.getSecurityID(),
//				pFundsFlowMarketData.getUpdateTime(),
//				pFundsFlowMarketData.getRetailBuyTurnover(),
//				pFundsFlowMarketData.getRetailBuyVolume(),
//				pFundsFlowMarketData.getRetailBuyAmount(),
//				pFundsFlowMarketData.getRetailSellTurnover(),
//				pFundsFlowMarketData.getRetailSellVolume(),
//				pFundsFlowMarketData.getRetailSellAmount(),
//				pFundsFlowMarketData.getMiddleBuyTurnover(),
//				pFundsFlowMarketData.getMiddleBuyVolume(),
//				pFundsFlowMarketData.getMiddleBuyAmount(),
//				pFundsFlowMarketData.getMiddleSellTurnover(),
//				pFundsFlowMarketData.getMiddleSellVolume(),
//				pFundsFlowMarketData.getMiddleSellAmount(),
//				pFundsFlowMarketData.getLargeBuyTurnover(),
//				pFundsFlowMarketData.getLargeBuyVolume(),
//				pFundsFlowMarketData.getLargeBuyAmount(),
//				pFundsFlowMarketData.getLargeSellTurnover(),
//				pFundsFlowMarketData.getLargeSellVolume(),
//				pFundsFlowMarketData.getLargeSellAmount(),
//				pFundsFlowMarketData.getInstitutionBuyTurnover(),
//				pFundsFlowMarketData.getInstitutionBuyVolume(),
//				pFundsFlowMarketData.getInstitutionBuyAmount(),
//				pFundsFlowMarketData.getInstitutionSellTurnover(),
//				pFundsFlowMarketData.getInstitutionSellVolume(),
//				pFundsFlowMarketData.getInstitutionSellAmount());
//	}
}


/**
 * @author 86158
 *
 */

