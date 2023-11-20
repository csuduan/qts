package org.qts.common.entity.trade;

import lombok.Data;
import org.qts.common.entity.Constant;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Enums.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 账户仓位
 */
@Data
public class Position implements Serializable {
    private String symbol; // 代码
    private String exchange; // 交易所代码
    private String symbolName;//合约名称


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

    private List<Trade> tradeList = new ArrayList<>();
    //暂时只允许同一时刻只有一个报单
    //private List<Order> pendingOrders = new ArrayList<>();
    private Order curOrder = null;


    public String getId() {
        return symbol + direction;
    }

    public Position(String symbol, POS_DIRECTION direction) {
        this.symbol = symbol;
        this.direction = direction;
    }

    public int getTotalPos(){
        int tradedVolume = 0;
        if(curOrder!=null){
            tradedVolume = curOrder.getOffset() ==OFFSET.OPEN?curOrder.getTradedVolume():-curOrder.getTradedVolume();
        }
        return this.totalPos+tradedVolume;
    }

    public void update(Order lastOrder) {
        if(curOrder ==null && !lastOrder.isFinished()){
            //保证每个order只被添加一次
            curOrder = lastOrder;
        }

        if(curOrder.isFinished()){
            //调整仓位
            if (curOrder.getTradedVolume()>0  && curOrder.getOffset() == OFFSET.OPEN) {
                this.tdPos = this.getTdPos() + curOrder.getTradedVolume();
            }
            if (curOrder.getTradedVolume()>0 ) {
                if(curOrder.getOffset() == OFFSET.OPEN)
                    this.tdPos = this.getTdPos() + curOrder.getTradedVolume();
                else if (curOrder.getOffset() == OFFSET.CLOSETD){
                    this.tdPos -= curOrder.getTotalVolume();
                }else{
                    //期货：上期所等同于平昨,其他期货交易所优先平今
                    //股票：？
                    if (Constant.EXCHANGE_SHFE.equals(curOrder.getExchange())){
                        this.ydPos -= curOrder.getTotalVolume();
                    }else{
                        this.tdPos -= curOrder.getTradedVolume();
                        if(this.tdPos< 0){
                            this.ydPos += this.ydPos;
                            this.tdPos =0 ;
                        }
                    }
                }
            }
            //矫正仓位(仓位不能为负数)
            if(this.tdPos<0)
                this.tdPos = 0;
            if(this.ydPos<0)
                this.ydPos = 0;

            this.curOrder = null;
        }
    }

    public void update(Trade trade) {
        this.tradeList.add(trade);
    }

}
