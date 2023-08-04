package org.mts.admin.entity;


public enum ReturnCode {
    SUCCESS(0,"成功"),
    SYS_ERROR(9999,"系统异常");

    int code;
    String message;

    private ReturnCode(int code, String message){
        this.code=code;
        this.message=message;
    }
    public int getCode(){
        return this.code;
    }
    public String getMessage(){
        return this.message;
    }
}
