package org.mts.common.model.acct;

import lombok.Data;

@Data
public class Position {

    private String id;//编号
    private String symbol;//合约代码
    private String direction;//方向
    private String exchange;//交易所
    private String symbolType;//合约类型
    private int multiple;//合约乘数

    // 持仓相关
    private int pos; // 持仓量
    private int onway; // 在途数量(>0在途开仓，<0在途平仓)
    private int ydPos; // 昨仓（=pos-tdPos）
    private int tdPos; // 今仓
    private int ydPosition;//昨仓(静态)

    // 盈亏相关
    private double avgPrice; // 持仓均价
    private double openPrice; //开仓均价
    private double holdProfit; //持仓盈亏
    private double lastPrice; //计算盈亏使用的行情最后价格
    private String updateTime;//更新时间

    private double useMargin; // 占用的保证金

}
