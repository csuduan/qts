package org.qts.common.gateway;


import org.qts.common.entity.Contract;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.trade.*;
import org.qts.common.entity.acct.*;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.acct.AcctPos;
import org.qts.common.entity.trade.CancelOrderReq;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Trade;


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
	boolean insertOrder(Order orderReq);

	/**
	 * 撤单
	 */
	void cancelOrder(CancelOrderReq cancelOrderReq);

	/**
	 * 发送持仓事件
	 * 
	 * @param position
	 */
	void emitPosition(AcctPos position);

	/**
	 * 发送账户事件
	 * 
	 * @param account
	 */
	void emitAccount(AcctInfo account);

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
	AcctInfo getAccount();
	void qryContract();

}
