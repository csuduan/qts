package com.bingbei.mts.trade.strategy.impl;

import com.bingbei.mts.common.entity.*;
import com.bingbei.mts.trade.engine.TradeExecutor;
import com.bingbei.mts.trade.strategy.StrategyAbstract;
import com.bingbei.mts.trade.strategy.StrategyDef;
import com.bingbei.mts.trade.strategy.StrategySetting;
import lombok.extern.slf4j.Slf4j;


/**
 * @author sun0x00@gmail.com
 */
@Slf4j
@StrategyDef("demo")
public class StrategyDemo extends StrategyAbstract {

	Long tickCount = 0L;
	Long barCount = 0L;

	double sumDelay1=0d;
	double sumDelay2=0d;
	long count=0L;

	public StrategyDemo(TradeExecutor tradeExecutor, StrategySetting strategySetting) {
		super(tradeExecutor, strategySetting);
	}

	@Override
	public void init()  {
		super.init();

		if(this.varMap.containsKey("tickCount")) {
			tickCount = Long.valueOf(this.varMap.get("tickCount"));
		}
		if(this.varMap.containsKey("barCount")) {
			barCount = Long.valueOf(this.varMap.get("barCount"));
		}
	}



	@Override
	public void onTick(Tick tick) throws Exception {
		super.onTick(tick);
		double tickDelay2=System.nanoTime()-tick.getTimeStampOnEvent();
		double tickDelay1=tick.getTimeStampOnEvent()-tick.getTimeStampRecv();
		sumDelay1+=tickDelay1/1000;
		sumDelay2+=tickDelay2/1000;
		count++;
		if(count%10==0)
			log.info("account:{} tick:{} {} delay1:{}us,delay2:{}us",this.strategySetting.getAccountId(),tick.getSymbol(),tick.getActionTime(),sumDelay1/count,sumDelay2/count);
		tickCount++;
		//setVarValue("tickCount", tickCount+"");

		if(tickCount>=1000){
			tickCount=0L;
			LocalPosition position=this.positionMap.get(tick.getSymbol());
			if(position.getLongPos()>0){
				//平多

			}else{
				//开多
			}
		}
	}

	@Override
	public void onBar(Bar bar) throws Exception {
		barCount++;
		setVarValue("barCount", barCount+"");

		
//		int tradeTime = bar.getDateTime().getMinute() + bar.getDateTime().getHour()*100;
//
//		String icSymbol = strategySetting.getContractByAlias("IC").getRtSymbol();
//		String ihSymbol = strategySetting.getContractByAlias("IH").getRtSymbol();
//		if(tradeTime == 945 && tradingStatus) {
//			if(bar.getRtSymbol().equals(icSymbol)) {
//				LocalPosition pd = positionMap.get(icSymbol);
//				int longPos = 0;
//				if( pd != null ) {
//					longPos = pd.getLongPos()+pd.getLongPosFrozen();
//					if(longPos<0) {
//						log.error("检测到仓位异常，longPos不应小于0");
//						stopTrading();
//						return;
//					}
//				}
//				int posDiff = 2 - longPos;
//				if(posDiff<0) {
//					log.error("仓位异常，应开仓数小于0");
//					stopTrading();
//					return;
//				}else if(posDiff == 0) {
//					log.warn("无需开仓");
//				}else {
//					//buy(icSymbol, posDiff, bar.getClose()+1, "a62c17309a8d4565a87b35792bbc1763.CNY.888888");
//				}
//
//			}else if(bar.getRtSymbol().equals(ihSymbol)) {
//				LocalPosition pd = positionMap.get(ihSymbol);
//				int shortPos = 0;
//				if( pd != null ) {
//					shortPos = pd.getShortPos()+pd.getShortPosFrozen();
//					if(shortPos<0) {
//						log.error("检测到仓位异常，shortPos不应小于0");
//						stopTrading();
//						return;
//					}
//				}
//				int posDiff = 2 - shortPos;
//				if(posDiff<0) {
//					log.error("仓位异常，应开仓数小于0");
//					stopTrading();
//					return;
//				}else if(posDiff == 0) {
//					log.warn("无需开仓");
//				}else {
//					//sellShort(ihSymbol, posDiff,  bar.getClose()-1, "a62c17309a8d4565a87b35792bbc1763.CNY.666666");
//				}
//
//			}
//		}else if(tradeTime == 1315 && tradingStatus) {
//			if(bar.getRtSymbol().equals(icSymbol)) {
//				LocalPosition pd = positionMap.get(icSymbol);
//				if( pd != null ) {
//					int longPos = pd.getLongPos()-pd.getLongPosFrozen();
//					if(longPos<0) {
//						log.error("检测到仓位异常，longPos不应小于0");
//						stopTrading();
//						return;
//					}else if(longPos == 0) {
//						log.error("无有效多头持仓");
//					}else {
//						//sell(icSymbol, longPos,  bar.getClose()-1, "a62c17309a8d4565a87b35792bbc1763.CNY.888888");
//					}
//				}else {
//					log.warn("无持仓");
//				}
//			}else if(bar.getRtSymbol().equals(ihSymbol)) {
//
//				LocalPosition pd = positionMap.get(ihSymbol);
//				if( pd != null ) {
//					int shortPos = pd.getShortPos()-pd.getShortPosFrozen();
//					if(shortPos<0) {
//						log.error("检测到仓位异常，shortPos不应小于0");
//						stopTrading();
//						return;
//					}else if(shortPos == 0) {
//						log.error("无有效多头持仓");
//					}else {
//						//buyToCover(ihSymbol, 2, bar.getClose()+1, "a62c17309a8d4565a87b35792bbc1763.CNY.666666");
//					}
//				}else {
//					log.warn("无持仓");
//				}
//			}
//		}else if(tradeTime == 1445 && tradingStatus) {
//			if(bar.getRtSymbol().equals(icSymbol)) {
//				LocalPosition pd = positionMap.get(icSymbol);
//				int longPos = 0;
//				if( pd != null ) {
//					longPos = pd.getLongPos()+pd.getLongPosFrozen();
//					if(longPos<0) {
//						log.error("检测到仓位异常，longPos不应小于0");
//						stopTrading();
//						return;
//					}
//				}
//				int posDiff = 2 - longPos;
//				if(posDiff<0) {
//					log.error("仓位异常，应开仓数小于0");
//					stopTrading();
//					return;
//				}else if(posDiff == 0) {
//					log.warn("无需开仓");
//				}else {
//					//buy(icSymbol, posDiff, bar.getClose()+1, "a62c17309a8d4565a87b35792bbc1763.CNY.888888");
//				}
//
//			}else if(bar.getRtSymbol().equals(ihSymbol)) {
//				LocalPosition pd = positionMap.get(ihSymbol);
//				int shortPos = 0;
//				if( pd != null ) {
//					shortPos = pd.getShortPos()+pd.getShortPosFrozen();
//					if(shortPos<0) {
//						log.error("检测到仓位异常，shortPos不应小于0");
//						stopTrading();
//						return;
//					}
//				}
//				int posDiff = 2 - shortPos;
//				if(posDiff<0) {
//					log.error("仓位异常，应开仓数小于0");
//					stopTrading();
//					return;
//				}else if(posDiff == 0) {
//					log.warn("无需开仓");
//				}else {
//					//sellShort(ihSymbol, posDiff,  bar.getClose()-1, "a62c17309a8d4565a87b35792bbc1763.CNY.666666");
//				}
//
//			}
//		}
		
	}


}
