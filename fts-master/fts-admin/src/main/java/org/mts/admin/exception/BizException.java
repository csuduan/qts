package org.mts.admin.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.mts.admin.entity.ReturnCode;

@Data
@AllArgsConstructor
public class BizException extends RuntimeException{
    public static  final int SYS_ERROR = 9999;
    private int errCode;
    private String errMsg;

    public BizException(String errMsg){
        this.errCode=SYS_ERROR;
        this.errMsg=errMsg;
    }
    public BizException(Exception ex){
        this.errCode=SYS_ERROR;
        this.errMsg=ex.getMessage();
    }

    public BizException(ReturnCode returnCode){
        this.errCode=returnCode.getCode();
        this.errMsg= returnCode.getMessage();
    }

}
