package com.bingbei.mts.trade.gateway.ctp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.entity.Tick;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import xyz.redtorch.gateway.ctp.x64v6v3v19p1v.api.*;

/**
 * @author sun0x00@gmail.com
 */
@Slf4j
public class MdSpi extends CThostFtdcMdSpi {

	private CThostFtdcMdApi cThostFtdcMdApi;
	private CtpMdGateway ctpGateway;
	private MdInfo mdInfo;
	private String tradingDayStr;
	private HashMap<String,Integer> preTickVolumeMap = new HashMap<>();
	private String mdName;

	public MdSpi(CtpMdGateway ctpGateway) {
		this.ctpGateway = ctpGateway;
		this.mdInfo =ctpGateway.getMdInfo();
		this.mdName=this.mdInfo.getId();
	}

	private boolean connectionStatus = false; // 前置机连接状态
	private boolean loginStatus = false; // 登陆状态

	/**
	 * 连接
	 */
	public synchronized void connect() {
		if (isConnected()) {
			return;
		}
		if (connectionStatus) {
			login();
			return;
		}

		log.warn("{} 行情接口实例初始化", mdName);
		
		String envTmpDir = System.getProperty("java.io.tmpdir");
		String tempFilePath = envTmpDir + File.separator + "mts"
				+ File.separator + "jctp" + File.separator + "TEMP_CTP" + File.separator + "MD_"
				+ mdName + "_";
		File tempFile = new File(tempFilePath);
		if (!tempFile.getParentFile().exists()) {
			try {
				FileUtils.forceMkdir(tempFile);
				log.info("{} 创建临时文件夹" , mdName, tempFile.getParentFile().getAbsolutePath());
			} catch (IOException e) {
				log.error("{} 创建临时文件夹失败", mdName, tempFile.getParentFile().getAbsolutePath());
			}
		}
		log.info("{} 使用临时文件夹", mdName, tempFile.getParentFile().getAbsolutePath());

		cThostFtdcMdApi = CThostFtdcMdApi.CreateFtdcMdApi(tempFile.getAbsolutePath());
		cThostFtdcMdApi.RegisterSpi(this);
		cThostFtdcMdApi.RegisterFront(mdInfo.getMdAddress());
		cThostFtdcMdApi.Init();

	}

	/**
	 * 关闭
	 */
	public synchronized void close() {
		if (cThostFtdcMdApi != null) {
			log.warn("{} 行情接口实例开始关闭并释放", mdName);
			cThostFtdcMdApi.RegisterSpi(null);
			
			// 避免异步线程找不到引用
			CThostFtdcMdApi cThostFtdcMdApiForRelease = cThostFtdcMdApi;
			// 由于CTP底层原因，部分情况下不能正确执行Release
			try {
				log.warn("行情接口释放启动！");
				cThostFtdcMdApiForRelease.Release();
				Thread.sleep(100);
			} catch (Exception e) {
				log.error("行情接口释放发生异常！", e);
			}

			cThostFtdcMdApi = null;
			connectionStatus = false;
			loginStatus = false;
			log.warn("{} 行情接口实例关闭并释放", mdName);
			// 通知停止其他关联实例
			ctpGateway.close();
		}else{
			log.warn("{} 行情接口实例为null,无需关闭", mdName);
		}
	}

	/**
	 * 返回接口状态
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return connectionStatus && loginStatus;
	}

	/**
	 * 获取交易日
	 * 
	 * @return
	 */
	public String getTradingDay() {
		return tradingDayStr;
	}

	/**
	 * 订阅行情
	 * 
	 * @param
	 */
	public void subscribe(String symbol) {
		if (isConnected()) {
			String[] symbolArray = new String[1];
			symbolArray[0] = symbol;
			cThostFtdcMdApi.SubscribeMarketData(symbolArray, 1);
		} else {
			log.warn(mdName + "无法订阅行情,行情服务器尚未连接成功");
		}
	}

	/**
	 * 退订行情
	 */
	public void unSubscribe(String symbol) {
		if (isConnected()) {
			String[] symbolArray = new String[1];
			symbolArray[0] = symbol;
			cThostFtdcMdApi.UnSubscribeMarketData(symbolArray, 1);
		} else {
			log.warn(mdName + "退订无效,行情服务器尚未连接成功");
		}
	}

	private void login() {
		// 登录
		CThostFtdcReqUserLoginField userLoginField = new CThostFtdcReqUserLoginField();
		cThostFtdcMdApi.ReqUserLogin(userLoginField, 0);
	}

	// 前置机联机回报
	public void OnFrontConnected() {
		log.info(mdName + "行情接口前置机已连接");
		// 修改前置机连接状态为true
		connectionStatus = true;
		login();
	}

	// 前置机断开回报
	public void OnFrontDisconnected(int nReason) {
		log.info(mdName + "行情接口前置机已断开,Reason:" + nReason);
		close();
	}

	// 登录回报
	public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo,
			int nRequestID, boolean bIsLast) {
		if (pRspInfo.getErrorID() == 0) {
			log.info("{}OnRspUserLogin! TradingDay:{},SessionID:{},BrokerID:{},UserID:{}", mdName,
					pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(),
					pRspUserLogin.getUserID());
			// 修改登录状态为true
			this.loginStatus = true;
			tradingDayStr = pRspUserLogin.getTradingDay();
			log.info("{}行情接口获取到的交易日为{}", mdName, tradingDayStr);
			// 重新订阅之前的合约
			if (!ctpGateway.getSubscribedSymbols().isEmpty()) {
				String[] subscribedSymbolsArray = ctpGateway.getSubscribedSymbols()
						.toArray(new String[ctpGateway.getSubscribedSymbols().size()]);
				cThostFtdcMdApi.SubscribeMarketData(subscribedSymbolsArray, subscribedSymbolsArray.length + 1);
			}
		} else {
			log.warn("{}行情接口登录回报错误! ErrorID:{},ErrorMsg:{}", mdName, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		}

	}

	// 心跳警告
	public void OnHeartBeatWarning(int nTimeLapse) {
		log.warn(mdName + "行情接口心跳警告 nTimeLapse:" + nTimeLapse);
	}

	// 登出回报
	public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID,
			boolean bIsLast) {
		if (pRspInfo.getErrorID() != 0) {
			log.info("{}OnRspUserLogout!ErrorID:{},ErrorMsg:{}", mdName, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		} else {
			log.info("{}OnRspUserLogout!BrokerID:{},UserID:{}", mdName, pUserLogout.getBrokerID(),
					pUserLogout.getUserID());

		}
		this.loginStatus = false;
	}

	// 错误回报
	public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		log.info("{}行情接口错误回报!ErrorID:{},ErrorMsg:{},RequestID:{},isLast{}", mdName, pRspInfo.getErrorID(),
				pRspInfo.getErrorMsg(), nRequestID, bIsLast);
	}

	// 订阅合约回报
	public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument,
			CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo.getErrorID() == 0) {
			log.info("{} OnRspSubMarketData! 订阅合约成功:{}", mdName,pSpecificInstrument.getInstrumentID());
		} else {
			log.error("{} OnRspSubMarketData! 订阅合约失败,ErrorID:{} ErrorMsg:{}", mdName, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}

	// 退订合约回报
	public void OnRspUnSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument,
			CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo.getErrorID() == 0) {
			log.info("{} OnRspSubMarketData! 退订合约成功:{}", mdName,pSpecificInstrument.getInstrumentID());
		} else {
			log.error("{} OnRspSubMarketData! 退订合约失败,ErrorID:{} ErrorMsg:{}", mdName, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}

	// 合约行情推送
	public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData) {
		if (pDepthMarketData != null) {

			String symbol = pDepthMarketData.getInstrumentID();

//			if (!contractExchangeMap.containsKey(symbol)) {
//				log.warn("{} 收到合约 {}行情,但尚未获取到交易所信息,丢弃",loginInfo.getAccoutId(),symbol);
//				return;
//			}

			// 上期所 郑商所正常,大商所错误
			// TODO 大商所时间修正
			Long updateTime = Long.valueOf(pDepthMarketData.getUpdateTime().replaceAll(":", ""));
			Long updateMillisec = (long) pDepthMarketData.getUpdateMillisec();
			double datetIime=updateTime+updateMillisec/1000.0;


			Tick tickData=new Tick();
			tickData.setSymbol(symbol);
			tickData.setExchange(pDepthMarketData.getExchangeID());
			tickData.setTradingDay(tradingDayStr);
			tickData.setActionDay(pDepthMarketData.getActionDay());
			tickData.setActionTime(datetIime);
			tickData.setStatus(0);
			tickData.setLastPrice(pDepthMarketData.getLastPrice());
			tickData.setVolume(pDepthMarketData.getVolume());

			Integer lastVolume = 0;
			if(preTickVolumeMap.containsKey(symbol)) {
				lastVolume = tickData.getVolume() - preTickVolumeMap.get(symbol);
			}else {
				lastVolume = tickData.getVolume();
			}
			tickData.setLastVolume(lastVolume);
			preTickVolumeMap.put(symbol, tickData.getVolume());
			tickData.setOpenInterest(pDepthMarketData.getOpenInterest());
			tickData.setPreOpenInterest((long)pDepthMarketData.getPreOpenInterest());
			tickData.setPreClosePrice(pDepthMarketData.getPreClosePrice());
			tickData.setPreSettlePrice(pDepthMarketData.getPreSettlementPrice());
			tickData.setOpenPrice(pDepthMarketData.getOpenPrice());
			tickData.setHighPrice(pDepthMarketData.getHighestPrice());
			tickData.setLowPrice(pDepthMarketData.getLowestPrice());
			tickData.setUpperLimit(pDepthMarketData.getUpperLimitPrice());
			tickData.setLowerLimit(pDepthMarketData.getLowerLimitPrice());
			tickData.setBidPrice1(pDepthMarketData.getBidPrice1());
			tickData.setBidPrice2(pDepthMarketData.getBidPrice2());
			tickData.setBidPrice3(pDepthMarketData.getBidPrice3());
			tickData.setBidPrice4(pDepthMarketData.getBidPrice4());
			tickData.setBidPrice5(pDepthMarketData.getBidPrice5());
			tickData.setAskPrice1(pDepthMarketData.getAskPrice1());
			tickData.setAskPrice2(pDepthMarketData.getAskPrice2());
			tickData.setAskPrice3(pDepthMarketData.getAskPrice3());
			tickData.setAskPrice4(pDepthMarketData.getAskPrice4());
			tickData.setAskPrice5(pDepthMarketData.getAskPrice5());
			tickData.setBidVolume1(pDepthMarketData.getBidVolume1());
			tickData.setBidVolume2(pDepthMarketData.getBidVolume2());
			tickData.setBidVolume3(pDepthMarketData.getBidVolume3());
			tickData.setBidVolume4(pDepthMarketData.getBidVolume4());
			tickData.setBidVolume5(pDepthMarketData.getBidVolume5());
			tickData.setAskVolume1(pDepthMarketData.getAskVolume1());
			tickData.setAskVolume2(pDepthMarketData.getAskVolume2());
			tickData.setAskVolume3(pDepthMarketData.getAskVolume3());
			tickData.setAskVolume4(pDepthMarketData.getAskVolume4());
			tickData.setAskVolume5(pDepthMarketData.getAskVolume5());

			log.info("tick:{}",tickData);
			//todo 对象池
			ctpGateway.emitTick(tickData);
		} else {
			log.warn("{}OnRtnDepthMarketData! 收到行情信息为空", mdName);
		}
	}

	// 订阅期权询价
	public void OnRspSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument,
			CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		log.info("{}OnRspSubForQuoteRsp!", mdName);
	}

	// 退订期权询价
	public void OnRspUnSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument,
			CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		log.info("{}OnRspUnSubForQuoteRsp!", mdName);
	}

	// 期权询价推送
	public void OnRtnForQuoteRsp(CThostFtdcForQuoteRspField pForQuoteRsp) {
		log.info("{}OnRspUnSubForQuoteRsp!", mdName);
	}

}