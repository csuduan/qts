package org.qts.trader.strategy;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class StrategySetting implements Serializable {

	private String strategyId; // 策略ID
	private String className;//策略实现类

	private Map<String, Object> paramMap = new HashMap<>();
	private List<String> contracts=new ArrayList<>();
}
