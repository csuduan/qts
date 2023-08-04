package org.mts.common.model.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class ReturnMsg {
    public static final ReturnMsg SUCCESS=new ReturnMsg(true,"成功");
    public static final ReturnMsg FAIL=new ReturnMsg(false,"失败");


    private boolean result;
    private String  message;
}
