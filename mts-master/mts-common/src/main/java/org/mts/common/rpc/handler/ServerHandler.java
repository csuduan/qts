package org.mts.common.rpc.handler;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.mts.common.model.Enums;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.listener.ServerListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private  Map<String, Channel> channelMap = new ConcurrentHashMap<>();
    public Map<String, Channel> getChannels(){
        return channelMap;
    }
    private ServerListener listener;

    public ServerHandler(ServerListener listener){
        this.listener=listener;
    }
    /**
     * 客户端连接会触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelMap.put(ctx.channel().id().toString(),ctx.channel());
        log.info("Channel active......");
        if(!this.channelMap.containsKey(ctx.channel().id().toString())){
            this.channelMap.put(ctx.channel().id().toString(),ctx.channel());
        }
        //ctx.channel().writeAndFlush("ping");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelMap.put(ctx.channel().id().toString(),ctx.channel());
        log.info("Channel Inactive......");
        if(this.channelMap.containsKey(ctx.channel().id().toString())){
            this.channelMap.remove(ctx.channel().id().toString());
        }
    }

    /**
     * 客户端发消息会触发
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("服务端收到消息: {}", msg.toString());
        Message request = JSON.parseObject(msg.toString(), Message.class);;
        //Attribute<String> attr = channel.attr(USERNAME_KEY)
        //message.setRid(ctx.channel().attr("uid").get());
        if(request.getType()!= Enums.MSG_TYPE.PING){
            Message response=listener.onRequest(request);
            String resMsg= JSON.toJSONString(response);
            ctx.channel().writeAndFlush(resMsg);
        }
//        else{
//            request.getSid();
//            ctx.channel().attr(AttributeKey.valueOf("userId")).set(request.getSid());
//        }
    }

    /**
     * 发生异常触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error:",cause);
        ctx.close();
    }

    public void reset(){
        channelMap.clear();
    }
}