package org.qts.common.entity.trade;


import lombok.Data;

/**
 * 交易指令(可以多次order)
 */
@Data
public class OrderInstr {
    private String acctId;
    private String contract;
    private String direction;
    private String offset;
    private double price;
    private int volume;
}
