package org.qts.trader.gateway;


import org.qts.common.entity.Contract;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.trade.CancelOrderReq;
import org.qts.common.entity.trade.Order;


public interface TdGateway {

	/**
	 * 连接
	 */
	void connect();

	/**
	 * 关闭
	 */
	void close();

	/**
	 * 发单
	 * 
	 * @param orderReq
	 */
	boolean insertOrder(Order orderReq);

	/**
	 * 撤单
	 */
	void cancelOrder(CancelOrderReq cancelOrderReq);

	/**
	 * 返回网关状态
	 * 
	 * @return
	 */
	boolean isConnected();

	LoginInfo getLoginInfo();
	Contract getContract(String symbol);
	void qryContract();

}
