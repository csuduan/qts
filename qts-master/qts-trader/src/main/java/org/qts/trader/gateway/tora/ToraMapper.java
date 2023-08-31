package org.qts.trader.gateway.tora;

import org.qts.common.entity.Constant;
import org.qts.common.entity.Enums;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tora.traderapi.traderapi.*;
import static ctp.thosttraderapi.thosttradeapiConstants.*;


/**
 * CTP常量转换器
 */
public class ToraMapper {
	public static Map<Enums.PRICE_TYPE,Character> priceTypeMap = new HashMap<>();
	public static Map<Character,Enums.PRICE_TYPE> priceTypeMapReverse = new HashMap<>();
	
	public static Map<Enums.TRADE_DIRECTION,Character> directionMap = new HashMap<>(){{
		put(Enums.TRADE_DIRECTION.BUY,getTORA_TSTP_D_Buy());
		put(Enums.TRADE_DIRECTION.SELL,getTORA_TSTP_D_Sell());

	}};
	public static Map<Character, Enums.TRADE_DIRECTION> directionMapReverse =
			directionMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));


	
	public static Map<String, Character> exchangeMap = new HashMap<>(){{
		put(Constant.EXCHANGE_SSE,getTORA_TSTP_EXD_SSE());
		put(Constant.EXCHANGE_SZSE,getTORA_TSTP_EXD_SZSE());
		put(Constant.EXCHANGE_HK,getTORA_TSTP_EXD_HK());
	}};
	public static Map<Character,String> exchangeMapReverse =
			exchangeMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));



	public static Map<Enums.POS_DIRECTION,Character> posiDirectionMap = new HashMap<>();
	public static Map<Character, Enums.POS_DIRECTION> posiDirectionMapReverse = new HashMap<>();
	
	public static Map<String,Character> productClassMap = new HashMap<>();
	public static Map<Character,String> productClassMapReverse = new HashMap<>();
	

	public static Map<Enums.ORDER_STATUS,Character> statusMap = new HashMap<>(){{
		put(Enums.ORDER_STATUS.UNKNOWN, getTORA_TSTP_OST_Unknown());
		put(Enums.ORDER_STATUS.NOTTRADED, getTORA_TSTP_OST_Accepted());
		put(Enums.ORDER_STATUS.PARTTRADED, getTORA_TSTP_OST_PartTraded());
		put(Enums.ORDER_STATUS.ALLTRADED, getTORA_TSTP_OST_AllTraded());
		put(Enums.ORDER_STATUS.PARTCANCELLED, getTORA_TSTP_OST_PartTradeCanceled());
		put(Enums.ORDER_STATUS.CANCELLED, getTORA_TSTP_OST_AllCanceled());
		put(Enums.ORDER_STATUS.ERROR, getTORA_TSTP_OST_Rejected());

	}};
	public static Map<Character,Enums.ORDER_STATUS> statusMapReverse =
			statusMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));


}
