package org.qts.trader.gateway.ctp;

import org.qts.common.entity.Constant;
import org.qts.common.entity.Enums;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static ctp.thosttraderapi.thosttradeapiConstants.*;

/**
 * CTP常量转换器
 */
public class CtpMapper {
	public static Map<Enums.PRICE_TYPE,Character> priceTypeMap = new HashMap<>();
	public static Map<Character,Enums.PRICE_TYPE> priceTypeMapReverse = new HashMap<>();
	
	public static Map<Enums.TRADE_DIRECTION,Character> directionMap = new HashMap<>();
	public static Map<Character, Enums.TRADE_DIRECTION> directionMapReverse = new HashMap<>();
	
	public static Map<Enums.OFFSET,Character> offsetMap = new HashMap<>();
	public static Map<Character,Enums.OFFSET> offsetMapReverse = new HashMap<>();
	
	public static Map<String,String> exchangeMap = new HashMap<>();
	public static Map<String,String> exchangeMapReverse = new HashMap<>();
	

	public static Map<Enums.POS_DIRECTION,Character> posiDirectionMap = new HashMap<>();
	public static Map<Character, Enums.POS_DIRECTION> posiDirectionMapReverse = new HashMap<>();
	
	public static Map<String,Character> productClassMap = new HashMap<>();
	public static Map<Character,String> productClassMapReverse = new HashMap<>();
	

	public static Map<Enums.ORDER_STATUS,Character> statusMap = new HashMap<>();
	public static Map<Character,Enums.ORDER_STATUS> statusMapReverse = new HashMap<>();
	
	static {
		
		// 价格类型映射
		priceTypeMap.put(Enums.PRICE_TYPE.LIMIT, THOST_FTDC_OPT_LimitPrice);
		priceTypeMap.put(Enums.PRICE_TYPE.MARKET, THOST_FTDC_OPT_AnyPrice);
		priceTypeMapReverse = priceTypeMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		
		// 方向类型映射
		directionMap.put(Enums.TRADE_DIRECTION.BUY, THOST_FTDC_D_Buy);
		directionMap.put(Enums.TRADE_DIRECTION.SELL, THOST_FTDC_D_Sell);
		directionMapReverse = directionMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		
		// 开平类型映射
		offsetMap.put(Enums.OFFSET.OPEN, THOST_FTDC_OF_Open);
		offsetMap.put(Enums.OFFSET.CLOSE, THOST_FTDC_OF_Close);
		offsetMap.put(Enums.OFFSET.CLOSETD, THOST_FTDC_OF_CloseToday);
		offsetMap.put(Enums.OFFSET.CLOSEYD, THOST_FTDC_OF_CloseYesterday);
		offsetMapReverse = offsetMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		
		// 交易所映射
		exchangeMap.put(Constant.EXCHANGE_CFFEX, "CFFEX");
		exchangeMap.put(Constant.EXCHANGE_SHFE, "SHFE");
		exchangeMap.put(Constant.EXCHANGE_CZCE, "CZCE");
		exchangeMap.put(Constant.EXCHANGE_DCE, "DCE");
		exchangeMap.put(Constant.EXCHANGE_SSE, "SSE");
		exchangeMap.put(Constant.EXCHANGE_SZSE, "SZSE");
		exchangeMap.put(Constant.EXCHANGE_INE, "INE");
		exchangeMapReverse = exchangeMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		
		//持仓类型映射
		posiDirectionMap.put(Enums.POS_DIRECTION.NET, THOST_FTDC_PD_Net);
		posiDirectionMap.put(Enums.POS_DIRECTION.LONG, THOST_FTDC_PD_Long);
		posiDirectionMap.put(Enums.POS_DIRECTION.SHORT, THOST_FTDC_PD_Short);
		posiDirectionMapReverse = posiDirectionMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		

		// 产品类型映射
		productClassMap.put(Constant.PRODUCT_FUTURES, THOST_FTDC_PC_Futures);
		productClassMap.put(Constant.PRODUCT_OPTION, THOST_FTDC_PC_Options);
		productClassMap.put(Constant.PRODUCT_COMBINATION, THOST_FTDC_PC_Combination);
		productClassMapReverse = productClassMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
//		v6.3.11不支持个股期权
//		productClassMapReverse.put(THOST_FTDC_PC_ETFOption, RtConstant.PRODUCT_OPTION);
//		productClassMapReverse.put(THOST_FTDC_PC_S, RtConstant.PRODUCT_EQUITY);
		
		// 委托状态映射
		statusMap.put(Enums.ORDER_STATUS.ALLTRADED, THOST_FTDC_OST_AllTraded);
		statusMap.put(Enums.ORDER_STATUS.PARTTRADED, THOST_FTDC_OST_PartTradedQueueing);
		statusMap.put(Enums.ORDER_STATUS.NOTTRADED, THOST_FTDC_OST_NoTradeQueueing);
		statusMap.put(Enums.ORDER_STATUS.CANCELLED, THOST_FTDC_OST_Canceled);
		statusMap.put(Enums.ORDER_STATUS.UNKNOWN, THOST_FTDC_OST_Unknown);
		statusMapReverse = statusMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		
		
		
	}
}
