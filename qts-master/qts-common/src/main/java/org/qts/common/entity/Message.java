package org.qts.common.entity;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.qts.common.entity.Enums.MSG_TYPE;

import java.util.List;

@Data
public  class Message {
    private String requestId;//请求编号
    private MSG_TYPE type;//消息类型
    private int  code; //0-成功
    private String data;//报文体
    public static final Message DEFAULT=new Message();

    public Message(){
    }
    public Message(MSG_TYPE type,Object data){
        this.type=type;
        if(data!=null){
            if(data instanceof String)
                this.data=(String)data;
            else
                this.data= JSON.toJSONString(data);
        }

    }

    public <T> T getData(Class<T> clazz){
        if(data==null)
            return null;
        else
            return JSON.parseObject(data,clazz);
    }
    public <T> List<T> getList(Class<T> clazz){
        if(data==null)
            return null;
        else
            return JSON.parseArray(data,clazz);
    }


    public Message buildResp(int code, Object data){
        Message response=new Message(Enums.MSG_TYPE.RETURN,data);
        response.setRequestId(this.requestId);
        response.setCode(code);
        return response;
    }
}
