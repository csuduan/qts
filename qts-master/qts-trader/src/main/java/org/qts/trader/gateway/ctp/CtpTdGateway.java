package org.qts.trader.gateway.ctp;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import ctp.thosttraderapi.*;
import lombok.Synchronized;
import org.apache.commons.io.FileUtils;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.entity.Constant;
import org.qts.common.entity.Contract;
import org.qts.common.entity.Enums;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.trade.CancelOrderReq;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Position;
import org.qts.common.entity.trade.Trade;
import lombok.extern.slf4j.Slf4j;
import org.qts.trader.gateway.TdGateway;
import org.springframework.util.StringUtils;

import static ctp.thosttraderapi.thosttradeapiConstants.*;
import static ctp.thosttraderapi.thosttradeapiConstants.THOST_FTDC_AF_Delete;


@Slf4j
public class CtpTdGateway extends CThostFtdcTraderSpi implements TdGateway {
	static {
		try {
			System.loadLibrary("thosttraderapi_wrap");
		} catch (Exception e) {
			log.error("加载库失败!", e);
		}
	}

	private String tdName;
	private AcctInfo acctInfo;

	protected LoginInfo loginInfo;
	protected FastQueue fastQueue;

	private CThostFtdcTraderApi tdApi;

	private boolean status = false; // 登陆状态
	private String tradingDay;

	private AtomicInteger reqID = new AtomicInteger(0); // 操作请求编号

	private int frontID = 0; // 前置机编号
	private int sessionID = 0; // 会话编号

	private HashMap<String, Position> positionMap = new HashMap<>();
	private Map<String, Contract> contractMap=new HashMap<>();
	private Map<String, Order> orderMap = new HashMap<>();

	private Timer timer = new Timer();


	public CtpTdGateway(AcctInfo acctInfo) {
		this.acctInfo = acctInfo;
		this.fastQueue = acctInfo.getFastQueue();
		this.loginInfo= new LoginInfo(acctInfo.getAcctConf());
		this.tdName = loginInfo.getUserId();
		log.info("交易接口{}开始初始化",loginInfo.getAcctId());
		timer.schedule(new QueryTimerTask(), new Date(), 1000);
		this.connect();
	}


	private void setStatus(boolean status){
		this.status = status;
	}

	/**
	 * 连接
	 */
	@Synchronized
	public synchronized void connect() {
		if (status==true ) {
			return;
		}

		new Thread(()->{
			log.warn("{} 交易接口实例初始化",tdName);
			String envTmpDir = System.getProperty("java.io.tmpdir");
			String tempFilePath = envTmpDir + File.separator + "qts" + File.separator + "td_"
					+ this.loginInfo.getAcctId();
			File tempFile = new File(tempFilePath);
			if (!tempFile.getParentFile().exists()) {
				try {
					FileUtils.forceMkdir(tempFile);
					log.info("{} 创建临时文件夹 {}", tdName, tempFile.getParentFile().getAbsolutePath());
				} catch (IOException e) {
					log.error("{} 创建临时文件夹失败{}", tdName, tempFile.getParentFile().getAbsolutePath(), e);
				}
			}
			log.info("{} 使用临时文件夹{}", tdName, tempFile.getParentFile().getAbsolutePath());
			tdApi = CThostFtdcTraderApi.CreateFtdcTraderApi(tempFile.getAbsolutePath());
			tdApi.RegisterSpi(this);
			tdApi.RegisterFront(loginInfo.getAddress());
			tdApi.Init();
		}).run();


	}

	/**
	 * 关闭
	 */
	@Synchronized
	public  void close() {
		if (tdApi != null) {
			try {
				log.warn("{} 交易接口实例开始关闭并释放", tdName);

				tdApi.RegisterSpi(null);

				log.warn("交易接口异步释放启动！");
				tdApi.Release();
			} catch (Exception e) {
				log.error("交易接口异步释放发生异常！", e);
			}
			tdApi = null;
			this.setStatus(false);
			log.warn("{} 交易接口实例关闭并异步释放", tdName);
		}else {
			log.warn("{} 交易接口实例为null,无需关闭",tdName);
		}

	}


	@Override
	public LoginInfo getLoginInfo() {
		return this.loginInfo;
	}

	@Override
	public Contract getContract(String symbol) {
		return null;
	}

	@Override
	public AcctDetail getAcct() {
		return null;
	}

	@Override
	public void qryContract() {

	}

	class QueryTimerTask extends TimerTask{

	    @Override
	    public void run() {
	    	try {
//				Thread.sleep(1250);
//		    	if(isConnected()) {
//			        queryAccount();
//		    	}
//			    Thread.sleep(1250);
//			    if(isConnected()) {
//				    queryPosition();
//			    }
			    Thread.sleep(1250);
	    	}catch (Exception e) {
				log.error(loginInfo.getAcctId()+"定时查询发生异常",e);
			}
	    }
	}



	/**
	 * 返回接口状态
	 *
	 * @return
	 */
	public boolean isConnected() {
		return  status;
	}

	/**
	 * 获取交易日
	 *
	 * @return
	 */
	public String getTradingDay() {
		return tradingDay;
	}

	/**
	 * 查询账户
	 */
	public void queryAccount() {
		if (tdApi == null) {
			log.info("{}尚未初始化,无法查询账户", tdName);
			return;
		}
		CThostFtdcQryTradingAccountField cThostFtdcQryTradingAccountField = new CThostFtdcQryTradingAccountField();
		tdApi.ReqQryTradingAccount(cThostFtdcQryTradingAccountField, reqID.incrementAndGet());
	}

	/**
	 * 查询持仓
	 */
	public void queryPosition() {
		if (tdApi == null) {
			log.info("{}尚未初始化,无法查询持仓", tdName);
			return;
		}


		this.positionMap.clear();
		CThostFtdcQryInvestorPositionField cThostFtdcQryInvestorPositionField = new CThostFtdcQryInvestorPositionField();
		// log.info("查询持仓");
		cThostFtdcQryInvestorPositionField.setBrokerID(loginInfo.getBrokerId());
		cThostFtdcQryInvestorPositionField.setInvestorID(loginInfo.getUserId());
		tdApi.ReqQryInvestorPosition(cThostFtdcQryInvestorPositionField, reqID.incrementAndGet());
	}

	/**
	 * 发单
	 *
	 * @param orderReq
	 * @return
	 */
	public boolean insertOrder(Order orderReq) {
		if (tdApi == null) {
			log.info("{}尚未初始化,无法发单", tdName);
			return false;
		}
		CThostFtdcInputOrderField cThostFtdcInputOrderField = new CThostFtdcInputOrderField();
		cThostFtdcInputOrderField.setInstrumentID(orderReq.getSymbol());
		cThostFtdcInputOrderField.setLimitPrice(orderReq.getPrice());
		cThostFtdcInputOrderField.setVolumeTotalOriginal(orderReq.getTotalVolume());

		cThostFtdcInputOrderField.setOrderPriceType(
				CtpConstant.priceTypeMap.getOrDefault(orderReq.getPriceType(), Character.valueOf('\0')));
		cThostFtdcInputOrderField
				.setDirection(CtpConstant.directionMap.getOrDefault(orderReq.getDirection(), Character.valueOf('\0')));
		cThostFtdcInputOrderField.setCombOffsetFlag(
				String.valueOf(CtpConstant.offsetMap.getOrDefault(orderReq.getOffset(), Character.valueOf('\0'))));
		cThostFtdcInputOrderField.setOrderRef(orderReq.getOrderRef());
		cThostFtdcInputOrderField.setInvestorID(loginInfo.getUserId());
		cThostFtdcInputOrderField.setUserID(loginInfo.getUserId());
		cThostFtdcInputOrderField.setBrokerID(loginInfo.getBrokerId());

		cThostFtdcInputOrderField
				.setCombHedgeFlag(String.valueOf(THOST_FTDC_HF_Speculation));
		cThostFtdcInputOrderField.setContingentCondition(THOST_FTDC_CC_Immediately);
		cThostFtdcInputOrderField.setForceCloseReason(THOST_FTDC_FCC_NotForceClose);
		cThostFtdcInputOrderField.setIsAutoSuspend(0);
		cThostFtdcInputOrderField.setTimeCondition(THOST_FTDC_TC_GFD);
		cThostFtdcInputOrderField.setVolumeCondition(THOST_FTDC_VC_AV);
		cThostFtdcInputOrderField.setMinVolume(1);

		// 判断FAK FOK市价单
		if (Constant.PRICETYPE_FAK.equals(orderReq.getPriceType())) {
			cThostFtdcInputOrderField.setOrderPriceType(THOST_FTDC_OPT_LimitPrice);
			cThostFtdcInputOrderField.setTimeCondition(THOST_FTDC_TC_IOC);
			cThostFtdcInputOrderField.setVolumeCondition(THOST_FTDC_VC_AV);
		} else if (Constant.PRICETYPE_FOK.equals(orderReq.getPriceType())) {
			cThostFtdcInputOrderField.setOrderPriceType(THOST_FTDC_OPT_LimitPrice);
			cThostFtdcInputOrderField.setTimeCondition(THOST_FTDC_TC_IOC);
			cThostFtdcInputOrderField.setVolumeCondition(THOST_FTDC_VC_CV);
		}

		tdApi.ReqOrderInsert(cThostFtdcInputOrderField, reqID.incrementAndGet());
		orderMap.put(orderReq.getOrderRef(),orderReq);
		return true;
	}

	// 撤单
	public void cancelOrder(CancelOrderReq cancelOrderReq) {

		if (tdApi == null) {
			log.info("{}尚未初始化,无法撤单", tdName);
			return;
		}
		CThostFtdcInputOrderActionField cThostFtdcInputOrderActionField = new CThostFtdcInputOrderActionField();

		cThostFtdcInputOrderActionField.setInstrumentID(cancelOrderReq.getSymbol());
		cThostFtdcInputOrderActionField.setExchangeID(cancelOrderReq.getExchange());
		cThostFtdcInputOrderActionField.setOrderRef(cancelOrderReq.getOrderID());
		cThostFtdcInputOrderActionField.setFrontID(cancelOrderReq.getFrontID());
		cThostFtdcInputOrderActionField.setSessionID(cancelOrderReq.getSessionID());

		cThostFtdcInputOrderActionField.setActionFlag(THOST_FTDC_AF_Delete);
		cThostFtdcInputOrderActionField.setBrokerID(loginInfo.getBrokerId());
		cThostFtdcInputOrderActionField.setInvestorID(loginInfo.getUserId());

		tdApi.ReqOrderAction(cThostFtdcInputOrderActionField, reqID.incrementAndGet());
	}

	private void login() {
		if(tdApi == null) {
			log.warn("{} 交易接口实例已经释放", tdName);
			return;
		}


		if (StringUtils.hasLength(loginInfo.getAuthCode())) {
			// 验证
			CThostFtdcReqAuthenticateField authenticateField = new CThostFtdcReqAuthenticateField();
			authenticateField.setAuthCode(loginInfo.getAuthCode());
			authenticateField.setUserID(loginInfo.getUserId());
			authenticateField.setBrokerID(loginInfo.getBrokerId());
			//authenticateField.setUserProductInfo(loginInfo.get());
			authenticateField.setAppID(loginInfo.getAppId());
			tdApi.ReqAuthenticate(authenticateField, reqID.incrementAndGet());
		} else {
			// 登录
			CThostFtdcReqUserLoginField userLoginField = new CThostFtdcReqUserLoginField();
			userLoginField.setBrokerID(loginInfo.getBrokerId());
			userLoginField.setUserID(loginInfo.getUserId());
			userLoginField.setPassword(loginInfo.getPassword());
			tdApi.ReqUserLogin(userLoginField, 0);
		}
	}

	// 前置机联机回报
	public void OnFrontConnected() {
		log.info("{} 交易接口前置机已连接",tdName);
		login();
	}

	// 前置机断开回报
	public void OnFrontDisconnected(int nReason) {
		log.info("{} 交易接口前置机已断开, Reason:{}", tdName ,nReason);
		//防止现场死锁，需要用异步线程进行close
		new Thread(this::close).run();
	}

	// 登录回报
	public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo,
							   int nRequestID, boolean bIsLast) {
		if (pRspInfo.getErrorID() == 0) {
			log.info("{} 交易接口登录成功! TradingDay:{},SessionID:{},BrokerID:{},UserID:{}", tdName,
					pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(),
					pRspUserLogin.getUserID());
			sessionID = pRspUserLogin.getSessionID();
			frontID = pRspUserLogin.getFrontID();
			// 修改登录状态为true
			this.setStatus(true);
			tradingDay = pRspUserLogin.getTradingDay();
			log.info("{}交易接口获取到的交易日为{}", tdName, tradingDay);

			// 确认结算单
			CThostFtdcSettlementInfoConfirmField settlementInfoConfirmField = new CThostFtdcSettlementInfoConfirmField();
			settlementInfoConfirmField.setBrokerID(loginInfo.getBrokerId());
			settlementInfoConfirmField.setInvestorID(loginInfo.getUserId());
			tdApi.ReqSettlementInfoConfirm(settlementInfoConfirmField, reqID.incrementAndGet());

		} else {
			log.error("{}交易接口登录回报错误! ErrorID:{},ErrorMsg:{}", tdName, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		}

	}

	// 心跳警告
	public void OnHeartBeatWarning(int nTimeLapse) {
		log.warn("{} 交易接口心跳警告, Time Lapse:{}", tdName, nTimeLapse);
	}

	// 登出回报
	public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID,
								boolean bIsLast) {
		if (pRspInfo.getErrorID() != 0) {
			log.info("{}OnRspUserLogout!ErrorID:{},ErrorMsg:{}", tdName, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		} else {
			log.info("{}OnRspUserLogout!BrokerID:{},UserID:{}", tdName, pUserLogout.getBrokerID(),
					pUserLogout.getUserID());

		}
		status = false;
	}

	// 错误回报
	public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		log.error("{} 交易接口错误回报!ErrorID:{},ErrorMsg:{},RequestID:{},isLast:{}", tdName, pRspInfo.getErrorID(),
				pRspInfo.getErrorMsg(), nRequestID, bIsLast);

	}

	// 验证客户端回报
	public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo,
								  int nRequestID, boolean bIsLast) {

		if (pRspInfo.getErrorID() == 0) {
			log.info(tdName + "交易接口客户端验证成功");
			login();

		} else {
			log.error("{}交易接口客户端验证失败! ErrorID:{},ErrorMsg:{}", tdName, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		}

	}

	public void OnRspUserPasswordUpdate(CThostFtdcUserPasswordUpdateField pUserPasswordUpdate,
										CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspTradingAccountPasswordUpdate(
			CThostFtdcTradingAccountPasswordUpdateField pTradingAccountPasswordUpdate, CThostFtdcRspInfoField pRspInfo,
			int nRequestID, boolean bIsLast) {
	}

	// 发单错误（柜台）
	public void OnRspOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID,
								 boolean bIsLast) {

		String orderRef = pInputOrder.getOrderRef();
		if(this.orderMap.containsKey(orderRef)){
			Order order=this.orderMap.get(orderRef);
			order.setStatus(Enums.ORDER_STATUS.ERROR);
			order.setStatusMsg(pRspInfo.getErrorMsg());
			this.fastQueue.emitEvent(FastEvent.EVENT_ORDER,order);
			// 发送委托事件
			log.error("{}交易接口发单错误回报(柜台)! ErrorID:{},ErrorMsg:{}", tdName, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		}

	}

	public void OnRspParkedOrderInsert(CThostFtdcParkedOrderField pParkedOrder, CThostFtdcRspInfoField pRspInfo,
									   int nRequestID, boolean bIsLast) {
	}

	public void OnRspParkedOrderAction(CThostFtdcParkedOrderActionField pParkedOrderAction,
									   CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	// 撤单错误回报（柜台）
	public void OnRspOrderAction(CThostFtdcInputOrderActionField pInputOrderAction, CThostFtdcRspInfoField pRspInfo,
								 int nRequestID, boolean bIsLast) {

		log.error("{}交易接口撤单错误（柜台）! ErrorID:{},ErrorMsg:{}", tdName, pRspInfo.getErrorID(),
				pRspInfo.getErrorMsg());
	}

	public void OnRspQueryMaxOrderVolume(CThostFtdcQueryMaxOrderVolumeField pQueryMaxOrderVolume,
										 CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	// 确认结算信息回报
	public void OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm,
										   CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {

		if (pRspInfo.getErrorID() == 0) {
			log.warn("{}交易接口结算信息确认完成!", tdName);
		} else {
			log.error("{}交易接口结算信息确认出错! ErrorID:{},ErrorMsg:{}", tdName, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		}

		// 查询所有合约
		log.warn("{}交易接口开始查询合约信息!", tdName);
		this.contractMap.clear();
		CThostFtdcQryInstrumentField cThostFtdcQryInstrumentField = new CThostFtdcQryInstrumentField();
		tdApi.ReqQryInstrument(cThostFtdcQryInstrumentField, reqID.incrementAndGet());

	}

	public void OnRspRemoveParkedOrder(CThostFtdcRemoveParkedOrderField pRemoveParkedOrder,
									   CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspRemoveParkedOrderAction(CThostFtdcRemoveParkedOrderActionField pRemoveParkedOrderAction,
											 CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspExecOrderInsert(CThostFtdcInputExecOrderField pInputExecOrder, CThostFtdcRspInfoField pRspInfo,
									 int nRequestID, boolean bIsLast) {
	}

	public void OnRspExecOrderAction(CThostFtdcInputExecOrderActionField pInputExecOrderAction,
									 CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspForQuoteInsert(CThostFtdcInputForQuoteField pInputForQuote, CThostFtdcRspInfoField pRspInfo,
									int nRequestID, boolean bIsLast) {
	}

	public void OnRspQuoteInsert(CThostFtdcInputQuoteField pInputQuote, CThostFtdcRspInfoField pRspInfo, int nRequestID,
								 boolean bIsLast) {
	}

	public void OnRspQuoteAction(CThostFtdcInputQuoteActionField pInputQuoteAction, CThostFtdcRspInfoField pRspInfo,
								 int nRequestID, boolean bIsLast) {
	}

	public void OnRspBatchOrderAction(CThostFtdcInputBatchOrderActionField pInputBatchOrderAction,
									  CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspOptionSelfCloseInsert(CThostFtdcInputOptionSelfCloseField pInputOptionSelfClose,
										   CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspOptionSelfCloseAction(CThostFtdcInputOptionSelfCloseActionField pInputOptionSelfCloseAction,
										   CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspCombActionInsert(CThostFtdcInputCombActionField pInputCombAction, CThostFtdcRspInfoField pRspInfo,
									  int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryOrder(CThostFtdcOrderField pOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID,
							  boolean bIsLast) {
	}

	public void OnRspQryTrade(CThostFtdcTradeField pTrade, CThostFtdcRspInfoField pRspInfo, int nRequestID,
							  boolean bIsLast) {
	}

	// 持仓查询回报
	public void OnRspQryInvestorPosition(CThostFtdcInvestorPositionField pInvestorPosition,
										 CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {

		if (pInvestorPosition == null || StringUtils.isEmpty(pInvestorPosition.getInstrumentID())) {
			return;
		}
		String symbol = pInvestorPosition.getInstrumentID();

		// 获取持仓缓存
		String posName = symbol + "-" + pInvestorPosition.getPosiDirection();

		Position position;
		if (positionMap.containsKey(posName)) {
			position = positionMap.get(posName);
		} else {
			position = new Position();
			positionMap.put(posName, position);
			position.setId(posName);
			position.setSymbol(symbol);
			position.setExchange(this.contractMap.get(symbol).getExchange());
			position.setMultiple(this.contractMap.get(symbol).getMultiple());

			position.setDirection(
					CtpConstant.posiDirectionMapReverse.getOrDefault(pInvestorPosition.getPosiDirection(), Enums.POS_DIRECTION.NONE));

			position.setAcctId(tdName);
		}

		position.setUseMargin(position.getUseMargin() + pInvestorPosition.getUseMargin());
		position.setExchangeMargin(position.getExchangeMargin() + pInvestorPosition.getExchangeMargin());

		position.setPosition(position.getPosition() + pInvestorPosition.getPosition());

		if (Constant.DIRECTION_LONG.equals(position.getDirection())) {
			position.setFrozen(pInvestorPosition.getShortFrozen());
		} else {
			position.setFrozen(pInvestorPosition.getLongFrozen());
		}

		if ("INE".contentEquals(position.getExchange()) || "SHFE".contentEquals(position.getExchange())) {
			// 针对上期所、上期能源持仓的今昨分条返回（有昨仓、无今仓）,读取昨仓数据
			if (pInvestorPosition.getYdPosition() > 0 && pInvestorPosition.getTodayPosition() == 0) {

				position.setYdPosition(position.getYdPosition() + pInvestorPosition.getPosition());

				if (Constant.DIRECTION_LONG.equals(position.getDirection())) {
					position.setYdFrozen(position.getYdFrozen() + pInvestorPosition.getShortFrozen());
				} else {
					position.setYdFrozen(position.getYdFrozen() + pInvestorPosition.getLongFrozen());
				}
			} else {
				position.setTdPosition(position.getTdPosition() + pInvestorPosition.getPosition());

				if (Constant.DIRECTION_LONG.equals(position.getDirection())) {
					position.setTdFrozen(position.getTdFrozen() + pInvestorPosition.getShortFrozen());
				} else {
					position.setTdFrozen(position.getTdFrozen() + pInvestorPosition.getLongFrozen());
				}
			}
		} else {
			position.setTdPosition(position.getTdPosition() + pInvestorPosition.getTodayPosition());
			position.setYdPosition(position.getPosition() - position.getTdPosition());

			// 中金所优先平今
			if ("CFFEX".equals(position.getExchange())) {
				if (position.getTdPosition() > 0) {
					if (position.getTdPosition() >= position.getFrozen()) {
						position.setTdFrozen(position.getFrozen());
					} else {
						position.setTdFrozen(position.getTdPosition());
						position.setYdFrozen(position.getFrozen() - position.getTdPosition());
					}
				} else {
					position.setYdFrozen(position.getFrozen());
				}
			} else {
				// 除了上面几个交易所之外的交易所，优先平昨
				if (position.getYdPosition() > 0) {
					if (position.getYdPosition() >= position.getFrozen()) {
						position.setYdFrozen(position.getFrozen());
					} else {
						position.setYdFrozen(position.getYdPosition());
						position.setTdFrozen(position.getFrozen() - position.getYdPosition());
					}
				} else {
					position.setTdFrozen(position.getFrozen());
				}
			}

		}

		// 计算成本
		Double cost = position.getAvgPrice() * position.getPosition() * position.getMultiple();
		Double openCost = position.getOpenPrice() * position.getPosition() * position.getMultiple();

		// 汇总总仓
		position.setPositionProfit(position.getPositionProfit() + pInvestorPosition.getPositionProfit());

		// 计算持仓均价
		if (position.getPosition() != 0 ) {
			position.setAvgPrice((cost + pInvestorPosition.getPositionCost()) / (position.getPosition() * position.getMultiple()));
			position.setOpenPrice((openCost + pInvestorPosition.getOpenCost()) / (position.getPosition() * position.getMultiple()));
		}

		// 回报结束
		if (bIsLast) {
			for (Position tmpPosition : positionMap.values()) {
				if(tmpPosition.getPosition()!=0) {
					tmpPosition.setPriceDiff(tmpPosition.getPositionProfit() / tmpPosition.getMultiple()
							/ tmpPosition.getPosition());

					if (Constant.DIRECTION_LONG.equals(tmpPosition.getDirection()) || (tmpPosition.getPosition() > 0
							&& Constant.DIRECTION_NET.equals(tmpPosition.getDirection()))) {

						// 计算最新价格
						tmpPosition.setLastPrice(tmpPosition.getAvgPrice() + tmpPosition.getPriceDiff());
						// 计算开仓价格差距
						tmpPosition.setOpenPriceDiff( tmpPosition.getLastPrice() - tmpPosition.getOpenPrice());
						// 计算开仓盈亏
						tmpPosition.setOpenPositionProfit(
								tmpPosition.getOpenPriceDiff() * tmpPosition.getPosition() * tmpPosition.getMultiple());

					} else if (Constant.DIRECTION_SHORT.equals(tmpPosition.getDirection())
							|| (tmpPosition.getPosition() < 0
							&& Constant.DIRECTION_NET.equals(tmpPosition.getDirection()))) {

						// 计算最新价格
						tmpPosition.setLastPrice(tmpPosition.getAvgPrice() - tmpPosition.getPriceDiff());
						// 计算开仓价格差距
						tmpPosition.setOpenPriceDiff(tmpPosition.getOpenPrice()-tmpPosition.getLastPrice());
						// 计算开仓盈亏
						tmpPosition.setOpenPositionProfit(
								tmpPosition.getOpenPriceDiff() * tmpPosition.getPosition() * tmpPosition.getMultiple());

					}else {
						log.error("{} 计算持仓时发现未处理方向，持仓详情{}",tdName,tmpPosition.toString());
					}

					// 计算保最新合约价值
					tmpPosition.setContractValue(tmpPosition.getLastPrice()
							* tmpPosition.getMultiple() * tmpPosition.getPosition());

					if (tmpPosition.getUseMargin() != 0) {
						tmpPosition.setPositionProfitRatio(tmpPosition.getPositionProfit() / tmpPosition.getUseMargin());
						tmpPosition.setOpenPositionProfitRatio(
								tmpPosition.getOpenPositionProfit() / tmpPosition.getUseMargin());

					}
				}
				// 发送持仓事件
				this.fastQueue.emitEvent(FastEvent.EVENT_POSITION,tmpPosition);

			}

			log.info("{} 持仓查询完毕,共计{}条",tdName,positionMap.size());
			// 清空缓存
			positionMap.clear();

		}

	}

	// 账户查询回报
	public void OnRspQryTradingAccount(CThostFtdcTradingAccountField pTradingAccount, CThostFtdcRspInfoField pRspInfo,
									   int nRequestID, boolean bIsLast) {
		AcctInfo account = new AcctInfo();
		account.setId(this.tdName);
		account.setAvailable(pTradingAccount.getAvailable());
		account.setCloseProfit(pTradingAccount.getCloseProfit());
		account.setCommission(pTradingAccount.getCommission());
		account.setMargin(pTradingAccount.getCurrMargin());
		account.setBalanceProfit(pTradingAccount.getPositionProfit());
		account.setPreBalance(pTradingAccount.getPreBalance());
		//account.setDeposit(pTradingAccount.getDeposit());
		//account.setWithdraw(pTradingAccount.getWithdraw());
		double balance = pTradingAccount.getPreBalance() - pTradingAccount.getPreCredit()
				- pTradingAccount.getPreMortgage() + pTradingAccount.getMortgage() - pTradingAccount.getWithdraw()
				+ pTradingAccount.getDeposit() + pTradingAccount.getCloseProfit() + pTradingAccount.getPositionProfit()
				+ pTradingAccount.getCashIn() - pTradingAccount.getCommission();
		account.setBalance(balance);
		this.fastQueue.emitEvent(FastEvent.EVENT_ACCT,account);

		log.info("{} 账户查询完毕,Avaliable:{},Balance:{}",tdName,account.getAvailable(),account.getBalance());
		try {
			Thread.sleep(1250);
		} catch (InterruptedException e) {
			log.error("sleep error",e);
		}
		queryPosition();
	}

	public void OnRspQryInvestor(CThostFtdcInvestorField pInvestor, CThostFtdcRspInfoField pRspInfo, int nRequestID,
								 boolean bIsLast) {
	}

	public void OnRspQryTradingCode(CThostFtdcTradingCodeField pTradingCode, CThostFtdcRspInfoField pRspInfo,
									int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryInstrumentMarginRate(CThostFtdcInstrumentMarginRateField pInstrumentMarginRate,
											 CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryInstrumentCommissionRate(CThostFtdcInstrumentCommissionRateField pInstrumentCommissionRate,
												 CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryExchange(CThostFtdcExchangeField pExchange, CThostFtdcRspInfoField pRspInfo, int nRequestID,
								 boolean bIsLast) {
	}

	public void OnRspQryProduct(CThostFtdcProductField pProduct, CThostFtdcRspInfoField pRspInfo, int nRequestID,
								boolean bIsLast) {
	}

	// 合约查询回报
	public void OnRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo,
								   int nRequestID, boolean bIsLast) {
		Contract contract = new Contract(pInstrument.getInstrumentID(),CtpConstant.exchangeMapReverse.get(pInstrument.getExchangeID()));

		contract.setMultiple(pInstrument.getVolumeMultiple());
		contract.setPriceTick(pInstrument.getPriceTick());
		contract.setStrikePrice(pInstrument.getStrikePrice());
		contract.setType(CtpConstant.productClassMapReverse.getOrDefault(pInstrument.getProductClass(),
				Constant.PRODUCT_UNKNOWN));
		contract.setExpiryDate(pInstrument.getExpireDate());
		// 针对商品期权
		contract.setUnderlyingSymbol(pInstrument.getUnderlyingInstrID());
		// contract.setUnderlyingSymbol(pInstrument.getUnderlyingInstrID()+pInstrument.getExpireDate().substring(2,
		// pInstrument.getExpireDate().length()-2));

		contractMap.put(contract.getSymbol(),contract);
		if (Constant.PRODUCT_OPTION.equals(contract.getType())) {
			if (pInstrument.getOptionsType() == '1') {
				contract.setOptionType(Constant.OPTION_CALL);
			} else if (pInstrument.getOptionsType() == '2') {
				contract.setOptionType(Constant.OPTION_PUT);
			}
		}

		this.fastQueue.emitEvent(FastEvent.EVENT_CONTRACT,contract);

		if (bIsLast) {
			log.info("{} 交易接口合约信息获取完成!共计{}条", tdName,contractMap.size());
			try {
				Thread.sleep(1250);
			} catch (InterruptedException e) {
				log.error("sleep error",e);
			}
			queryAccount();
		}

	}

	public void OnRspQryDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData,
										CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQrySettlementInfo(CThostFtdcSettlementInfoField pSettlementInfo, CThostFtdcRspInfoField pRspInfo,
									   int nRequestID, boolean bIsLast) {
	}


	public void OnRspQryInvestorPositionDetail(CThostFtdcInvestorPositionDetailField pInvestorPositionDetail,
											   CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryNotice(CThostFtdcNoticeField pNotice, CThostFtdcRspInfoField pRspInfo, int nRequestID,
							   boolean bIsLast) {
	}

	public void OnRspQrySettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm,
											  CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	// 委托回报
	public void OnRtnOrder(CThostFtdcOrderField pOrder) {
		if(!orderMap.containsKey(pOrder.getOrderRef())){
			log.warn("外部RtnOrder  {}",pOrder.getOrderRef());
			return;
		}
		Order order=orderMap.get(pOrder.getOrderRef());
//		String newRef = pOrder.getOrderRef().trim();
//		// 更新最大报单编号
//		orderRef = new AtomicInteger(Math.max(orderRef.get(), Integer.valueOf(newRef)));
//
//		String symbol = pOrder.getInstrumentID();
//		String exchange = CtpConstant.exchangeMapReverse.get(pOrder.getExchangeID());
//		/*
//		 * CTP的报单号一致性维护需要基于frontID, sessionID, orderID三个字段
//		 * 但在本接口设计中,已经考虑了CTP的OrderRef的自增性,避免重复 唯一可能出现OrderRef重复的情况是多处登录并在非常接近的时间内（几乎同时发单
//		 */
//		//String orderID = pOrder.getOrderRef();
//		String direction = CtpConstant.directionMapReverse.get(pOrder.getDirection());
//		String offset = CtpConstant.offsetMapReverse.get(pOrder.getCombOffsetFlag().toCharArray()[0]);
//		double price = pOrder.getLimitPrice();
//		int totalVolume = pOrder.getVolumeTotalOriginal();
//		int tradedVolume = pOrder.getVolumeTraded();
//		String status = CtpConstant.statusMapReverse.get(pOrder.getOrderStatus());
//		String tradingDay = tradingDayStr;
//		String orderDate = pOrder.getInsertDate();
//		String orderTime = pOrder.getInsertTime();
//		String cancelTime = pOrder.getCancelTime();
//		String activeTime = pOrder.getActiveTime();
//		String updateTime = pOrder.getUpdateTime();
//		int frontID = pOrder.getFrontID();
//		int sessionID = pOrder.getSessionID();

		order.setStatus(CtpConstant.statusMapReverse.get(pOrder.getOrderStatus()));
		order.setTradedVolume(pOrder.getVolumeTotalOriginal());
		order.setUpdateTime(pOrder.getUpdateTime());
		fastQueue.emitEvent(FastEvent.EVENT_ORDER,order);
	}

	// 成交回报
	public void OnRtnTrade(CThostFtdcTradeField pTrade) {
		if(!this.orderMap.containsKey(pTrade.getOrderRef()))
			return;
		//Order order=this.orderMap.get(pTrade.getOrderRef());

		Trade trade=new Trade();
		trade.setAccountID(this.loginInfo.getAcctId());
		trade.setSymbol(pTrade.getInstrumentID());
		trade.setExchange(pTrade.getExchangeID());
		trade.setOrderRef(pTrade.getOrderRef());
		trade.setTradeID(pTrade.getTradeID());
		trade.setDirection(CtpConstant.directionMapReverse.getOrDefault(pTrade.getDirection(), Enums.TRADE_DIRECTION.NONE));
		trade.setOffset(CtpConstant.offsetMapReverse.getOrDefault(pTrade.getOffsetFlag(), Enums.OFFSET.NONE));
		trade.setPrice(pTrade.getPrice());
		trade.setVolume(pTrade.getVolume());
		trade.setTradeDate(pTrade.getTradeDate());
		trade.setTradeTime(pTrade.getTradeTime());
		fastQueue.emitEvent(FastEvent.EVENT_TRADE,trade);
	}

	// 发单错误回报（交易所）
	public void OnErrRtnOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo) {
		String orderRef = pInputOrder.getOrderRef();
		if(this.orderMap.containsKey(orderRef)){
			Order order=this.orderMap.get(orderRef);
			order.setStatus(Enums.ORDER_STATUS.ERROR);
			order.setStatusMsg(pRspInfo.getErrorMsg());
			fastQueue.emitEvent(FastEvent.EVENT_ORDER,order);
			// 发送委托事件
			log.error("{}交易接口发单错误回报（交易所）! ErrorID:{},ErrorMsg:{}", tdName, pRspInfo.getErrorID(),
					pRspInfo.getErrorMsg());
		}

	}

	// 撤单错误回报（交易所）
	public void OnErrRtnOrderAction(CThostFtdcOrderActionField pOrderAction, CThostFtdcRspInfoField pRspInfo) {
		log.error("{}交易接口撤单错误回报（交易所）! ErrorID:{},ErrorMsg:{}", tdName, pRspInfo.getErrorID(),
				pRspInfo.getErrorMsg());
	}
}
