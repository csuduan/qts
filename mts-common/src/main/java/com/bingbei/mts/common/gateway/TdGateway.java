package com.bingbei.mts.common.gateway;


import com.bingbei.mts.common.entity.*;

/**
 * @author sun0x00@gmail.com
 */
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
	String insertOrder(Order orderReq);

	/**
	 * 撤单
	 */
	void cancelOrder(CancelOrderReq cancelOrderReq);

	/**
	 * 发送持仓事件
	 * 
	 * @param position
	 */
	void emitPosition(Position position);

	/**
	 * 发送账户事件
	 * 
	 * @param account
	 */
	void emitAccount(Account account);

	/**
	 * 发送合约事件
	 * 
	 * @param contract
	 */
	void emitContract(Contract contract);



	/**
	 * 发送成交事件
	 * 
	 * @param trade
	 */
	void emitTrade(Trade trade);


	/**
	 * 发送委托事件
	 * 
	 * @param order
	 */
	void emitOrder(Order order);


	/**
	 * 返回网关状态
	 * 
	 * @return
	 */
	boolean isConnected();

	LoginInfo getLoginInfo();
	Contract getContract(String symbol);
	Account getAccount();
	void qryContract();

}
