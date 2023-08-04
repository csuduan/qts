package org.mts.admin.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 账户
 */
@Data
public class Account implements Serializable {

	private static final long serialVersionUID = 6823946394104654905L;

	private LoginInfo loginInfo;//登录信息

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



	//账户持仓(用于校验)
	private Map<String, AccoPosition> accoPositionMap = new HashMap<>();
}
