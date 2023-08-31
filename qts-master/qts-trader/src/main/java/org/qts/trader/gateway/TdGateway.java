package org.qts.trader.gateway;


import org.qts.common.entity.Contract;
import org.qts.common.entity.trade.OrderCancelReq;
import org.qts.common.entity.trade.Order;


public interface TdGateway {

	/**
	 * 连接
	 */
	void connect();

	/**
	 * 关闭接口
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
	void cancelOrder(OrderCancelReq cancelOrderReq);

	/**
	 * 返回网关状态
	 * 
	 * @return
	 */
	boolean isConnected();

	Contract getContract(String symbol);

	//查询类接口
	void qryContract();
	void qryPosition();
	void qryTradingAccount();

}
