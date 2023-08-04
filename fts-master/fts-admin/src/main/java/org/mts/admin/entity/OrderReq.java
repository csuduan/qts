package org.mts.admin.entity;

import lombok.Data;

@Data
public class OrderReq {
    private String accoundId;
    private String contract;
    private String direction;
    private String offset;
    private double price;
    private int volume;

}
