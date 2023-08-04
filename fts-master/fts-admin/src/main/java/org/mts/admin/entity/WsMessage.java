package org.mts.admin.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WsMessage<T> {
    private int type;
    private T data;

}
