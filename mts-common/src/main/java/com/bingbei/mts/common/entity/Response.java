package com.bingbei.mts.common.entity;

import com.bingbei.mts.common.enums.ReturnCode;
import lombok.Data;

@Data
public class Response <T>{
    private int returnCode;
    private String returnMsg;
    private T body;

    public Response(){
        this.returnCode= ReturnCode.SUCCESS.getCode();
        this.returnMsg=ReturnCode.SUCCESS.getMessage();
    }
    public Response(ReturnCode returnCode){
        this.returnCode=returnCode.getCode();
        this.returnMsg=returnCode.getMessage();
    }

    public Response(int returnCode,String returnMsg){
        this.returnCode=returnCode;
        this.returnMsg=returnMsg;
    }

}
