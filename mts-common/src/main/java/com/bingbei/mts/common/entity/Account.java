package com.bingbei.mts.common.entity;

import com.bingbei.mts.common.gateway.TdGateway;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class Account implements Serializable {

	private static final long serialVersionUID = 6823946394104654905L;

	private LoginInfo loginInfo;

	private String id;//账户编号，唯一标识
	private String name;//账户名称
	private String currency; // 币种

	// 资产相关
	private double preBalance; // 昨日账户结算净值
	private double balance; // 账户净值
	private double available; // 可用资金
	private double commission; // 今日手续费
	private double margin; // 保证金占用
	private double closeProfit; // 平仓盈亏
	private double positionProfit; // 持仓盈亏
	private double deposit; // 入金
	private double withdraw; // 出金


	//合约信息
	private Map<String, Contract> contractMap = new HashMap<>();
	//报单与持仓
	private Map<String, Position> positionMap = new HashMap<>();

	private Map<String, Order> orderMap = new HashMap<>();
	private Map<String, Order> workingOrderMap = new HashMap<>();
	private Map<String, Trade> tradeMap = new HashMap<>();
	private Map<String, LocalPositionDetail> localPositionDetailMap = new HashMap<>();

	private TdGateway tdGateway;


	public LocalPositionDetail createLocalPositionDetail(String accountID, String symbol) {
		String positionDetailKey = symbol + "." + accountID;
		LocalPositionDetail localPositionDetail = new LocalPositionDetail(){

		};
		localPositionDetailMap.put(positionDetailKey, localPositionDetail);
		return localPositionDetail;

	}
}
