package org.qts.trader.strategy.impl;

import org.qts.common.entity.Enums;
import org.qts.common.entity.trade.*;
import org.qts.trader.core.StrategyEngine;
import org.qts.trader.strategy.AbsStrategy;
import org.qts.trader.strategy.StrategySetting;

import java.util.ArrayList;
import java.util.List;

public class StrTrendKzz1 extends AbsStrategy {
    private String symbolKzz;
    private String symbolStock;
    private int len1; //ma len
    private int targetVolume;
    private double stoploss;
    private List<Tick> ticksKzz =new ArrayList<>();
    private List<Tick> ticksStock =new ArrayList<>();


    //以下计算过程中使用的临时变量
    private double lastWprice = 0;
    private double sumVol = 0;
    private List<Double> sumDvps=new ArrayList<>();
    private List<Double> maDvps=new ArrayList<>();
    private List<Double> dsls=new ArrayList<>();


    private boolean posFlag = false;
    private double openPrice;


    @Override
    public void init(StrategyEngine strategyEngine,StrategySetting strategySetting) {
        super.init(strategyEngine,strategySetting);
        symbolKzz = strategySetting.getContracts().get(0);
        symbolStock = strategySetting.getContracts().get(1);
        len1 = (int) strategySetting.getParamMap().get("len1");
        targetVolume = (int) strategySetting.getParamMap().get("targetVolume");

    }

    @Override
    public void onTick(Tick tick) throws Exception {
        if(tick.getSymbol().equals(symbolKzz)){
            ticksKzz.add(tick);
        }else
            return;

        if(ticksKzz.size()<2)
            return;
        sumVol += tick.getVolume();

        double dwp= wprice(tick)-lastWprice;//价格波动
        double dvol = tick.getVolume()-ticksKzz.get(ticksKzz.size()-2).getVolume();//成交量波动
        double ptt = sumVol/ticksKzz.size();
        double dvp=dwp*(dvol/ptt);
        double lastSumDvp = sumDvps.get(sumDvps.size()-1) + dvp;
        sumDvps.add(lastSumDvp);

        //cal sma for  sumDvps
        double maDvp = sma(sumDvps,len1);
        maDvps.add(maDvp);
        double dsl = 0;
        //cal dsl
        if(maDvps.size()<60)
            dsl =maDvp-maDvps.get(0);
        else
            dsl = maDvp-maDvps.get(maDvps.size()-60);

        dsls.add(dsl);
        double upper = 0;
        double lower = 0;
        //cal bollinger(dsl,len1,2)
        double bollinger[] = calBollinger(dsls,len1);
        upper = bollinger[0]+2*bollinger[1];
        lower = bollinger[0]-2*bollinger[1];

        //交易信号处理
        if(!posFlag  && dsl >upper){
            //做多转债，发对价买单
            posFlag = true;
            openPrice = tick.getAskPrice1();
            this.open(symbolKzz, Enums.TRADE_DIRECTION.BUY,openPrice,targetVolume);
        }else if(posFlag && dsl < lower  ){
            //平仓
            posFlag = false;
            this.close(symbolKzz, Enums.TRADE_DIRECTION.SELL,tick.getBidPrice1(),targetVolume);
        }else if(posFlag&& openPrice-wprice(tick)>stoploss){
            //止损
            posFlag = false;
            this.close(symbolKzz, Enums.TRADE_DIRECTION.SELL,tick.getBidPrice1(),targetVolume);
        }
    }

    private double wprice(Tick tick){
        return tick.getBidPrice1()+(tick.getAskPrice1()-tick.getBidPrice1())*(tick.getBidVolume1()/(tick.getBidVolume1()+tick.getAskVolume1()));
    }

    private double sma(List<Double> list,int len){
        double sum = 0;
        if(list.size()<len){
            for(int i=0;i<list.size();i++){
                sum += list.get(i);
            }
            return sum/list.size();
        }else{
            for(int i=list.size()-len;i<list.size();i++){
                sum += list.get(i);
            }
            return sum/len;
        }
    }

    private double[] calBollinger(List<Double> list, int len){
        double ma;
        double std;
        double sum = 0;
        if(list.size()<len){
            for(int i=0;i<list.size();i++){
                sum += list.get(i);
            }
            ma = sum/list.size();
            double sum2 = 0;
            for(int i=0;i<list.size();i++){
                sum2 += Math.pow(list.get(i)-ma,2);
            }
            std= Math.sqrt(sum2/list.size());
        }else{
            for(int i=list.size()-len;i<list.size();i++){
                sum += list.get(i);
            }
            ma = sum/len;
            double sum2 = 0;
            for(int i=list.size()-len;i<list.size();i++){
                sum2 += Math.pow(list.get(i)-ma,2);
            }
            std= Math.sqrt(sum2/len);
        }
        return new double[]{ma,std};
    }
}
