package org.qts.common.rpc.tcp.server;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {

    //private static AttributeKey<Boolean> ATTRI_LOGIN = AttributeKey.newInstance("login");
    //channelId-Channel Map
    private  Map<String, Channel> channelMap = new ConcurrentHashMap<>();
    //已登录channel

    private MsgHandler msgHandler;

    public Map<String, Channel> getChannels(){
        return channelMap;
    }

    public ServerHandler(MsgHandler handler){
        this.msgHandler=handler;
    }
    /**
     * 客户端连接会触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String channelId=ctx.channel().id().toString();
        channelMap.put(channelId,ctx.channel());
        this.channelMap.put(channelId,ctx.channel());
        log.info("Channel[{}] active......,total connections:{}",channelId,this.channelMap.size());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String channelId=ctx.channel().id().toString();
        if(this.channelMap.containsKey(channelId)){
            this.channelMap.remove(channelId);
        }
        log.info("Channel[{}] Inactive......,left connections:{}",channelId,this.channelMap.size());
    }

    /**
     * 客户端发消息会触发
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("recv msg: {}", msg.toString());
        Message request = JSON.parseObject(msg.toString(), Message.class);;
//        if(request.getType() == Enums.MSG_TYPE.LOGIN){
//            String acctId = request.getAcctId();
//            ctx.channel().attr(ATTRI_LOGIN).set(true);
//            ctx.channel().attr(ATTRI_ACCT_ID).set(acctId);
//
//            acctChannelMap.put(acctId,ctx.channel());
//            listener.onStatus(acctId,true);
//        }else if(request.getType()!= Enums.MSG_TYPE.PING){
//            //通用处理
//            Message response=listener.onRequest(request);
//            String resMsg= JSON.toJSONString(response);
//            ctx.channel().writeAndFlush(resMsg);
//        }

        if(request.getType()!= Enums.MSG_TYPE.PING){
            //通用处理
            Message response=msgHandler.onRequest(request);
            String resMsg= JSON.toJSONString(response);
            ctx.channel().writeAndFlush(resMsg);
            log.info("send msg: {}", resMsg);

        }else{
            Message response=request.buildResp(0,null);
            String resMsg= JSON.toJSONString(response);
            ctx.channel().writeAndFlush(resMsg);
            log.info("send msg: {}", resMsg);

        }
    }

    /**
     * 发生异常触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error:",cause);
        ctx.close();
    }

    public void push(Message message){
        this.channelMap.values().forEach(channel -> {
            if(channel.isActive()){
                String msgStr= JSON.toJSONString(message);
                channel.writeAndFlush(msgStr);
            }
        });
    }
}