package org.qts.common.entity.acct;

import lombok.Data;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.Position;
import org.qts.common.entity.trade.Trade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账户详情
 */
@Data
public class AcctDetail  extends AcctInfo implements Serializable{

	private LoginInfo loginInfo;//登录信息
	private Map<String,Position> positions =new HashMap<>();
	private List<Trade> tradeList = new ArrayList<>();

	public  AcctDetail(AcctConf conf){
		super(conf);
	}

}
