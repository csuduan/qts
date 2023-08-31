package org.qts.common.entity;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;


public interface Constant {
	String GATEWAY_CTP = "CTP";
	String GATEWAY_OST = "OST";
	String GATEWAY_REM = "REM";



	// 方向常量
	String DIRECTION_LONG = "LONG"; // 多
	String DIRECTION_SHORT = "SHORT"; // 空
	String DIRECTION_NET = "NET";//净


	// 开平常量
	String OFFSET_OPEN = "OPEN"; // 开仓
	String OFFSET_CLOSE = "CLOSE"; // 平仓
	String OFFSET_CLOSETODAY = "CLOSETODAY"; // 平今
	String OFFSET_CLOSEYESTERDAY = "CLOSEYESTERDAY"; // 平昨
	String OFFSET_UNKNOWN =  "UNKNOWN";

	// 状态常量
	String STATUS_NOTTRADED = "NOTTRADED"; // 未成交
	String STATUS_PARTTRADED = "PARTTRADED"; // 部分成交
	String STATUS_ALLTRADED = "ALLTRADED"; // 全部成交
	String STATUS_CANCELLED = "CANCELLED"; // 已撤销
	String STATUS_REJECTED = "REJECTED"; // 拒单
	String STATUS_UNKNOWN = "UNKNOWN"; // 未知

//	HashSet<String> STATUS_FINISHED = new HashSet<String>() {
//		private static final long serialVersionUID = 8777691797309945190L;
//		{
//			add(Constant.STATUS_REJECTED);
//			add(Constant.STATUS_CANCELLED);
//			add(Constant.STATUS_ALLTRADED);
//		}
//	};

	HashSet<String> STATUS_WORKING = new HashSet<String>() {
		private static final long serialVersionUID = 909683985291870766L;
		{
			add(Constant.STATUS_UNKNOWN);
			add(Constant.STATUS_NOTTRADED);
			add(Constant.STATUS_PARTTRADED);
		}
	};

	// 合约类型常量
	String PRODUCT_STOCK = "STOCK"; // 股票
	String PRODUCT_FUTURES = "FUTURES"; // 期货
	String PRODUCT_OPTION = "OPTION"; // 期权
	String PRODUCT_INDEX = "INDEX"; // 指数
	String PRODUCT_COMBINATION = "COMBINATION"; // 组合
	String PRODUCT_FOREX = "FOREX"; // 外汇
	String PRODUCT_ETF = "ETF"; // ETF
	String PRODUCT_BOND = "BOND"; // 债券
	String PRODUCT_UNKNOWN = "UNKNOWN"; // 未知

	// bar level
	String BAR_M1 = "M1";
	String BAR_M5 = "M5";
	String BAR_D1 = "D1";


	// 价格类型常量
	String PRICETYPE_LIMITPRICE = "LIMITPRICE"; // 限价
	String PRICETYPE_MARKETPRICE = "MARKETPRICE "; // 市价
	String PRICETYPE_FAK = "FAK"; // FAK
	String PRICETYPE_FOK = "FOK"; // FOK

	// 期权类型
	String OPTION_CALL = "CALL"; // 看涨期权
	String OPTION_PUT = "PUT"; // 看跌期权

	// 交易所类型
	 String EXCHANGE_SSE = "SSE"; // 上交所
	 String EXCHANGE_SZSE = "SZSE"; // 深交所
	 String EXCHANGE_CFFEX = "CFFEX"; // 中金所
	 String EXCHANGE_SHFE = "SHFE"; // 上期所
	 String EXCHANGE_CZCE = "CZCE"; // 郑商所
	 String EXCHANGE_DCE = "DCE"; // 大商所
	 String EXCHANGE_INE = "INE"; // 国际能源交易中心
	 String EXCHANGE_HK = "HK"; // 港交所
	 String EXCHANGE_HKFE = "HKFE"; // 香港期货交易所
	 String EXCHANGE_SGX = "SGX"; // 新加坡交易所
	 String EXCHANGE_CME = "CME"; // 芝商所
	 String EXCHANGE_ICE = "ICE"; // 洲际交易所
	 String EXCHANGE_IPE = "IPE"; // 洲际交易所
	 String EXCHANGE_LME = "LME"; // 伦敦金属交易所



	 String DT_FORMAT_WITH_MS = "yyyy-MM-dd HH:mm:ss.SSS";
	 String DT_FORMAT_WITH_MS_INT = "yyyyMMddHHmmssSSS";
	 String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";
	 String DT_FORMAT_INT = "yyyyMMddHHmmss";

	 String T_FORMAT_WITH_MS_INT = "HHmmssSSS";
	 String T_FORMAT_WITH_MS = "HH:mm:ss.SSS";
	 String T_FORMAT_INT = "HHmmss";
	 String T_FORMAT = "HH:mm:ss";
	 String D_FORMAT_INT = "yyyyMMdd";
	 String D_FORMAT = "yyyy-MM-dd";
}
