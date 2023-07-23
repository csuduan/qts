package org.mts.common.model.rpc;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.mts.common.model.Enums;

import java.util.List;

@Data
public class Message {
    private String requestId;//请求编号
    private String sid;//发送对象
    private String rid;//接收对象
    private String acctId;//关联账户
    private Enums.MSG_TYPE type;//消息类型
    private Boolean success;
    private String data;//报文体

    public Message(){
    }
    public Message(Enums.MSG_TYPE type,Object data){
        this.type=type;
        if(data!=null){
            if(data instanceof String)
                this.data=(String)data;
            else
                this.data= JSON.toJSONString(data);
        }

    }

    public Message(Enums.MSG_TYPE type,String acctId,Object data){
        this.type=type;
        this.acctId=acctId;
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

    public static final Message DEFAULT=new Message();

    public Message buildResp(boolean result,Object data){
        Message response=new Message(Enums.MSG_TYPE.RETURN,data);
        response.setRequestId(this.requestId);
        response.setSuccess(result);
        return response;
    }
}
