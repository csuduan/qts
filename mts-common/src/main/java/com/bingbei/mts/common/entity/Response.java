package com.bingbei.mts.common.entity;

import com.bingbei.mts.common.enums.ReturnCode;
import lombok.Data;

@Data
public class Response <T>{
    private int code;
    private String message;
    private T data;

    public Response(){
        this.code = ReturnCode.SUCCESS.getCode();
        this.message =ReturnCode.SUCCESS.getMessage();
    }
    public Response(ReturnCode returnCode){
        this.code =returnCode.getCode();
        this.message =returnCode.getMessage();
    }

    public Response(int returnCode,String returnMsg){
        this.code =returnCode;
        this.message =returnMsg;
    }

}
