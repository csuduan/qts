package org.qts.common.entity.trade;

import lombok.Data;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Enums.*;

import java.io.Serializable;

/**
 * 账户仓位
 */
@Data
public class Position implements Serializable {
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String symbolName ;//合约名称


	private int multiple = 1;//合约乘数
	// 持仓相关
	private POS_DIRECTION direction; // 持仓方向
	private int totalPos; // 总持仓量
	private int totalFrozen; // 总冻结数量
	private int ydPos; //昨持仓
	private int ydFrozen; // 冻结数量
	private int tdPos; // 今持仓
	private int tdFrozen; // 冻结数量
	private int prePos;//昨日余额(盘中不变)

	private double useMargin; // 占用的保证金
	private double exchangeMargin; // 交易所的保证金
	private double contractValue; // 最新合约价值

	//收益相关
	private double lastPrice; // 计算盈亏使用的行情最后价格
	private double avgPrice; // 持仓均价
	private double priceDiff; // 持仓价格差
	private double openPrice; // 开仓均价
	private double openPriceDiff; // 开仓价格差
	private double positionProfit; // 持仓盈亏
	private double positionProfitRatio; // 持仓盈亏率
	private double openPositionProfit; // 开仓盈亏
	private double openPositionProfitRatio; // 开仓盈亏率
	private double commission;//手续费(今)

	public String getId(){
		return symbol+"-"+direction;
	}
	public Position(){

	}

	public Position(String symbol,String exchange,POS_DIRECTION direction){
		this.symbol = symbol;
		this.exchange = exchange;
		this.direction = direction;
	}

	public void update(Order lastOrder){
		int curTradedVolume = lastOrder.getTradedVolume()- lastOrder.getLastTradedVolume();
		if(curTradedVolume>0 && lastOrder.getDirection() == Enums.TRADE_DIRECTION.BUY ){
			this.tdPos = this.getTdPos() + curTradedVolume;
		}
		if(curTradedVolume>0 && lastOrder.getDirection() == Enums.TRADE_DIRECTION.SELL ){
			//优先平昨(部分券运行平今)
			int left = this.ydPos-curTradedVolume;
			this.ydPos = left>=0?left:0;
			if(left<0)
				this.tdPos += left;
		}
	}
}
