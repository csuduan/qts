package com.bingbei.mts.adapter.ctp;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;

import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.entity.RtConstant;
import com.bingbei.mts.common.entity.Tick;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import xyz.redtorch.gateway.ctp.x64v6v3v19p1v.api.*;

/**
 * @author sun0x00@gmail.com
 */
@Slf4j
public class MdSpi extends CThostFtdcMdSpi {

	private xyz.redtorch.gateway.ctp.x64v6v3v19p1v.api.CThostFtdcMdApi cThostFtdcMdApi;
	private CtpMdGateway ctpGateway;
	private MdInfo mdInfo;
	private String tradingDayStr;
	private HashMap<String,Integer> preTickVolumeMap = new HashMap<>();
	private String mdName;

	MdSpi(CtpMdGateway ctpGateway) {
		this.ctpGateway = ctpGateway;
		this.mdInfo =ctpGateway.getMdInfo();
		this.mdName=this.mdInfo.getId();
		this.connect();
	}

	private boolean connectionStatus = false; // 前置机连接状态
	private boolean loginStatus = false; // 登陆状态

	/**
	 * 连接
	 */
	private synchronized void connect() {
		if (isConnected()) {
			return;
		}
		if (connectionStatus) {
			login();
			return;
		}

		log.warn("{} 行情接口实例初始化", mdInfo);
		
		String envTmpDir = System.getProperty("java.io.tmpdir");
		String tempFilePath = envTmpDir + File.separator + "mts"
				+ File.separator + "jctp" + File.separator + "TEMP_CTP" + File.separator + "MD_"
				+ mdInfo + "_";
		File tempFile = new File(tempFilePath);
		if (!tempFile.getParentFile().exists()) {
			try {
				//FileUtils.forceMkdirParent(tempFile);
				FileUtils.forceMkdir(tempFile);
				log.info("{} 创建临时文件夹" , mdInfo, tempFile.getParentFile().getAbsolutePath());
			} catch (IOException e) {
				log.error("{} 创建临时文件夹失败", mdInfo, tempFile.getParentFile().getAbsolutePath());
			}
		}
		log.info("{} 使用临时文件夹", mdInfo, tempFile.getParentFile().getAbsolutePath());

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
			log.warn("{} 行情接口实例开始关闭并释放", mdInfo);
			cThostFtdcMdApi.RegisterSpi(null);
			
			// 避免异步线程找不到引用
			CThostFtdcMdApi cThostFtdcMdApiForRelease = cThostFtdcMdApi;
			// 由于CTP底层原因，部分情况下不能正确执行Release
			new Thread() {
				public void run() {
					
					Thread.currentThread().setName("网关ID-"+ mdInfo+"行情接口异步释放线程"+LocalDateTime.now().format(RtConstant.DT_FORMAT_WITH_MS_FORMATTER));
					
					try {
						log.warn("行情接口异步释放启动！");
						cThostFtdcMdApiForRelease.Release();
					} catch (Exception e) {
						log.error("行情接口异步释放发生异常！", e);
					}
				}
			}.start();
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// nop
			}
			cThostFtdcMdApi = null;
			connectionStatus = false;
			loginStatus = false;
			log.warn("{} 行情接口实例关闭并释放", mdInfo);
			// 通知停止其他关联实例
			ctpGateway.close();
		}else{
			log.warn("{} 行情接口实例为null,无需关闭", mdInfo);
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
			log.warn(mdInfo + "无法订阅行情,行情服务器尚未连接成功");
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
			log.warn(mdInfo + "退订无效,行情服务器尚未连接成功");
		}
	}

	private void login() {
		// 登录
		CThostFtdcReqUserLoginField userLoginField = new CThostFtdcReqUserLoginField();
		//userLoginField.setBrokerID(brokerID);
		//userLoginField.setUserID(userID);
		//userLoginField.setPassword(password);
		cThostFtdcMdApi.ReqUserLogin(userLoginField, 0);
	}

	// 前置机联机回报
	public void OnFrontConnected() {
		log.info(mdInfo + "行情接口前置机已连接");
		// 修改前置机连接状态为true
		connectionStatus = true;
		login();
	}

	// 前置机断开回报
	public void OnFrontDisconnected(int nReason) {
		log.info(mdInfo + "行情接口前置机已断开,Reason:" + nReason);
		close();
	}

	// 登录回报
	public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo,
			int nRequestID, boolean bIsLast) {
		if (pRspInfo.getErrorID() == 0) {
			log.info("{}OnRspUserLogin! TradingDay:{},SessionID:{},BrokerID:{},UserID:{}", mdInfo,
					pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(),
					pRspUserLogin.getUserID());
			// 修改登录状态为true
			this.loginStatus = true;
			tradingDayStr = pRspUserLogin.getTradingDay();
			log.info("{}行情接口获取到的交易日为{}", mdInfo, tradingDayStr);
			// 重新订阅之前的合约
			if (!ctpGateway.getSubscribedSymbols().isEmpty()) {
				String[] subscribedSymbolsArray = ctpGateway.getSubscribedSymbols()
						.toArray(new String[ctpGateway.getSubscribedSymbols().size()]);
				cThostFtdcMdApi.SubscribeMarketData(subscribedSymbolsArray, subscribedSymbolsArray.length + 1);
			}
		} else {
			log.warn("{}行情接口登录回报错误! ErrorID:{},ErrorMsg:{}", mdInfo, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		}

	}

	// 心跳警告
	public void OnHeartBeatWarning(int nTimeLapse) {
		log.warn(mdInfo + "行情接口心跳警告 nTimeLapse:" + nTimeLapse);
	}

	// 登出回报
	public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID,
			boolean bIsLast) {
		if (pRspInfo.getErrorID() != 0) {
			log.info("{}OnRspUserLogout!ErrorID:{},ErrorMsg:{}", mdInfo, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		} else {
			log.info("{}OnRspUserLogout!BrokerID:{},UserID:{}", mdInfo, pUserLogout.getBrokerID(),
					pUserLogout.getUserID());

		}
		this.loginStatus = false;
	}

	// 错误回报
	public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		log.info("{}行情接口错误回报!ErrorID:{},ErrorMsg:{},RequestID:{},isLast{}", mdInfo, pRspInfo.getErrorID(),
				pRspInfo.getErrorMsg(), nRequestID, bIsLast);
	}

	// 订阅合约回报
	public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument,
			CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo.getErrorID() == 0) {
			log.info("{} OnRspSubMarketData! 订阅合约成功:{}", mdInfo,pSpecificInstrument.getInstrumentID());
		} else {
			log.error("{} OnRspSubMarketData! 订阅合约失败,ErrorID:{} ErrorMsg:{}", mdInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
		}
	}

	// 退订合约回报
	public void OnRspUnSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument,
			CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo.getErrorID() == 0) {
			log.info("{} OnRspSubMarketData! 退订合约成功:{}", mdInfo,pSpecificInstrument.getInstrumentID());
		} else {
			log.error("{} OnRspSubMarketData! 退订合约失败,ErrorID:{} ErrorMsg:{}", mdInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
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
			Long actionDay = Long.valueOf(pDepthMarketData.getActionDay());

			String updateDateTimeWithMS = (actionDay * 100 * 100 * 100 * 1000 + updateTime * 1000 + updateMillisec)
					+ "";

			LocalDateTime dateTime;
			try {
				//todo 耗时操作
				dateTime = LocalDateTime.parse(updateDateTimeWithMS,RtConstant.DT_FORMAT_WITH_MS_INT_FORMATTER);
				//dateTime = RtConstant.DT_FORMAT_WITH_MS_INT_FORMATTER.parseDateTime(updateDateTimeWithMS);
			} catch (Exception e) {
				log.error("{}解析日期发生异常", mdInfo, e);
				return;
			}

			//String exchange = contractExchangeMap.get(symbol);
			//String rtSymbol = symbol + "." + exchange;
			String exchange=pDepthMarketData.getExchangeID();
			String tradingDay = tradingDayStr;
			String actionDayStr = pDepthMarketData.getActionDay();
			String actionTime = dateTime.format(RtConstant.T_FORMAT_WITH_MS_INT_FORMATTER);
			Integer status = 0;
			Double lastPrice = pDepthMarketData.getLastPrice();
			Integer volume = pDepthMarketData.getVolume();
			Integer lastVolume = 0;
			if(preTickVolumeMap.containsKey(symbol)) {
				lastVolume = volume - preTickVolumeMap.get(symbol);
			}else {
				lastVolume = volume;
			}
			preTickVolumeMap.put(symbol, volume);
			Double openInterest = pDepthMarketData.getOpenInterest();
			Long preOpenInterest = (long) pDepthMarketData.getPreOpenInterest();
			Double preClosePrice = pDepthMarketData.getPreClosePrice();
			Double preSettlePrice = pDepthMarketData.getPreSettlementPrice();
			Double openPrice = pDepthMarketData.getOpenPrice();
			Double highPrice = pDepthMarketData.getHighestPrice();
			Double lowPrice = pDepthMarketData.getLowestPrice();
			Double upperLimit = pDepthMarketData.getUpperLimitPrice();
			Double lowerLimit = pDepthMarketData.getLowerLimitPrice();
			Double bidPrice1 = pDepthMarketData.getBidPrice1();
			Double bidPrice2 = pDepthMarketData.getBidPrice2();
			Double bidPrice3 = pDepthMarketData.getBidPrice3();
			Double bidPrice4 = pDepthMarketData.getBidPrice4();
			Double bidPrice5 = pDepthMarketData.getBidPrice5();
			Double bidPrice6 = 0d;
			Double bidPrice7 = 0d;
			Double bidPrice8 = 0d;
			Double bidPrice9 = 0d;
			Double bidPrice10 = 0d;
			Double askPrice1 = pDepthMarketData.getAskPrice1();
			Double askPrice2 = pDepthMarketData.getAskPrice2();
			Double askPrice3 = pDepthMarketData.getAskPrice3();
			Double askPrice4 = pDepthMarketData.getAskPrice4();
			Double askPrice5 = pDepthMarketData.getAskPrice5();
			Double askPrice6 = 0d;
			Double askPrice7 = 0d;
			Double askPrice8 = 0d;
			Double askPrice9 = 0d;
			Double askPrice10 = 0d;
			Integer bidVolume1 = pDepthMarketData.getBidVolume1();
			Integer bidVolume2 = pDepthMarketData.getBidVolume2();
			Integer bidVolume3 = pDepthMarketData.getBidVolume3();
			Integer bidVolume4 = pDepthMarketData.getBidVolume4();
			Integer bidVolume5 = pDepthMarketData.getBidVolume5();
			Integer bidVolume6 = 0;
			Integer bidVolume7 = 0;
			Integer bidVolume8 = 0;
			Integer bidVolume9 = 0;
			Integer bidVolume10 = 0;
			Integer askVolume1 = pDepthMarketData.getAskVolume1();
			Integer askVolume2 = pDepthMarketData.getAskVolume2();
			Integer askVolume3 = pDepthMarketData.getAskVolume3();
			Integer askVolume4 = pDepthMarketData.getAskVolume4();
			Integer askVolume5 = pDepthMarketData.getAskVolume5();
			Integer askVolume6 = 0;
			Integer askVolume7 = 0;
			Integer askVolume8 = 0;
			Integer askVolume9 = 0;
			Integer askVolume10 = 0;

			//todo 对象池
			Tick tickData=new Tick();
			ctpGateway.emitTick(tickData);

//			ctpGateway.emitTick(gatewayID, gatewayDisplayName, symbol, exchange, rtSymbol, contractName, tickID,
//					tradingDay, actionDayStr, actionTime, dateTime, status, lastPrice, lastVolume, volume, openInterest,
//					preOpenInterest, preClosePrice, preSettlePrice, openPrice, highPrice, lowPrice, upperLimit,
//					lowerLimit, bidPrice1, bidPrice2, bidPrice3, bidPrice4, bidPrice5, bidPrice6, bidPrice7, bidPrice8,
//					bidPrice9, bidPrice10, askPrice1, askPrice2, askPrice3, askPrice4, askPrice5, askPrice6, askPrice7,
//					askPrice8, askPrice9, askPrice10, bidVolume1, bidVolume2, bidVolume3, bidVolume4, bidVolume5,
//					bidVolume6, bidVolume7, bidVolume8, bidVolume9, bidVolume10, askVolume1, askVolume2, askVolume3,
//					askVolume4, askVolume5, askVolume6, askVolume7, askVolume8, askVolume9, askVolume10);

		} else {
			log.warn("{}OnRtnDepthMarketData! 收到行情信息为空", mdInfo);
		}
	}

	// 订阅期权询价
	public void OnRspSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument,
			CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		log.info("{}OnRspSubForQuoteRsp!", mdInfo);
	}

	// 退订期权询价
	public void OnRspUnSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument,
			CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		log.info("{}OnRspUnSubForQuoteRsp!", mdInfo);
	}

	// 期权询价推送
	public void OnRtnForQuoteRsp(CThostFtdcForQuoteRspField pForQuoteRsp) {
		log.info("{}OnRspUnSubForQuoteRsp!", mdInfo);
	}

}