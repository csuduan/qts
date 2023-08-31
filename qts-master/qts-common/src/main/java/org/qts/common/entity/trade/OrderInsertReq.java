package org.qts.common.entity.trade;

import lombok.Data;
import org.qts.common.entity.Enums;

@Data
public class OrderInsertReq {
    private String symbol;
    private String exchange;
    private Enums.TRADE_DIRECTION direction;
    private Enums.OFFSET offset;
    private double price;
    private int volume;

}
