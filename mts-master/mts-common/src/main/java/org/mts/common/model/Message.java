package org.mts.common.model;

import lombok.Data;

@Data
public class Message<T> {
    private String sid;//接收对象
    private String rid;//发送对象
    private Enums.MSG_TYPE type;//消息类型
    private T data;//消息体

}
