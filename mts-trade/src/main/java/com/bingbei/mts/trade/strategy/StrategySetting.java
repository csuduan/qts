package com.bingbei.mts.trade.strategy;

import com.bingbei.mts.common.entity.SubscribeReq;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class StrategySetting implements Serializable {

	private static final long serialVersionUID = 4037995985601670824L;

	private String accountId;//账户代码
	private String strategyId; // 策略ID
	private String className;//策略实现类

	private Map<String, String> paramMap = new HashMap<>();
	private List<String> contracts=new ArrayList<>();
}
