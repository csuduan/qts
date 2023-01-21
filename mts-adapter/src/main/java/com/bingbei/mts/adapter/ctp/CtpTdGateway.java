package com.bingbei.mts.adapter.ctp;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import com.bingbei.mts.common.entity.*;
import com.bingbei.mts.common.gateway.TdGatewayAbstract;
import com.bingbei.mts.common.service.FastEventEngineService;
import com.bingbei.mts.common.service.extend.event.EventConstant;
import com.bingbei.mts.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * @author sun0x00@gmail.com
 */
@Slf4j
public class CtpTdGateway extends TdGatewayAbstract {

	private Timer timer = new Timer();
	//private HashMap<String,Contract> contractHashMap=new HashMap<>();
	private TdSpi tdSpi = null;

	public CtpTdGateway(FastEventEngineService fastEventEngineService, LoginInfo loginInfo) {
		super(fastEventEngineService, loginInfo);
		timer.schedule(new QueryTimerTask(), new Date(), 1000);
	}



	@Override
	public void connect() {
		if (tdSpi != null) {
			tdSpi.close();
		}
		tdSpi = new TdSpi(this);
		tdSpi.connect();
	}

	@Override
	public void close() {
		// 务必判断连接状态，防止死循环
		if (tdSpi != null&&tdSpi.isConnected()) {
			tdSpi.close();
		}

		// 在这里发送事件主要是由于接口可能自动断开，需要广播通知
		fastEventEngineService.emitSimpleEvent(EventConstant.EVENT_GATEWAY, EventConstant.EVENT_GATEWAY,null);
	}

	@Override
	public String insertOrder(Order orderReq) {
		if (tdSpi != null) {
			return tdSpi.insertOrder(orderReq);
		} else {
			return null;
		}

	}

	@Override
	public void cancelOrder(CancelOrderReq cancelOrderReq) {
		if (tdSpi != null) {
			tdSpi.cancelOrder(cancelOrderReq);
		}
	}

	public void queryAccount() {
		if (tdSpi != null) {
			tdSpi.queryAccount();
		}
	}

	public void queryPosition() {
		if (tdSpi != null) {
			tdSpi.queryPosition();
		}
	}

	@Override
	public boolean isConnected() {
		return tdSpi != null  && tdSpi.isConnected() ;
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
	public Account getAccount() {
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
				log.error(loginInfo.getAccoutId()+"定时查询发生异常",e);
			}
	    }
	}


}
