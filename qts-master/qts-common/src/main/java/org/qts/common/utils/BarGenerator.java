package org.qts.common.utils;


import org.qts.common.entity.Constant;
import org.qts.common.entity.trade.Bar;
import org.qts.common.entity.trade.Tick;

/**
 * 1分钟Bar生成器
 */
public class BarGenerator {

	private Bar lastBar ;
	private Tick lastTick = null;
	CommonBarCallBack commonBarCallBack;

	public BarGenerator(CommonBarCallBack commonBarCallBack) {
		this.commonBarCallBack = commonBarCallBack;
	}

	/**
	 * 更新Tick数据
	 *
	 * @param tick
	 */
	public void updateTick(Tick tick) {
		//todo 暂时只处理一分钟K线
		if (lastTick != null) {
			// 此处过滤重复或者乱序tick
			if (tick.getTimes() <= lastTick.getTimes()) {
				return;
			}
		}

		if (lastBar == null) {
			lastBar = this.buildBar(tick, Constant.BAR_M1);
		} else if (lastBar.getUpdateTime() != (long)tick.getActionTime()/100) {
			// 回调OnBar方法
			commonBarCallBack.call(lastBar);
			lastBar = this.buildBar(tick,Constant.BAR_M1);
		}else {
			lastBar.setHigh(Math.max(lastBar.getHigh(), tick.getLastPrice()));
			lastBar.setLow(Math.min(lastBar.getLow(), tick.getLastPrice()));
			lastBar.setClose(tick.getLastPrice());
		}
		lastTick = tick;
	}

	private Bar buildBar(Tick tick,String level){
		Bar bar=new Bar();
		bar.setLevel(level);
		bar.setSymbol(tick.getSymbol());
		bar.setTradingDay(tick.getTradingDay());
		bar.setDate(tick.getActionDay());
		bar.setBarTime((long)tick.getActionTime()/100);
		bar.setOpen(tick.getLastPrice());
		bar.setLow(tick.getLastPrice());
		bar.setHigh(tick.getLastPrice());
		bar.setClose(tick.getLastPrice());
		return bar;
	}

	/**
	 * CallBack接口,用于注册Bar生成器回调事件
	 */
	public static interface CommonBarCallBack {
		void call(Bar bar);
	}
}

