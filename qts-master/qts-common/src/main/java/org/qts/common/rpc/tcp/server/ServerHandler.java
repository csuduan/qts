package org.qts.common.rpc.tcp.server;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {

    private static AttributeKey<Boolean> ATTRI_LOGIN = AttributeKey.newInstance("login");
    private static AttributeKey<String> ATTRI_ACCT_ID = AttributeKey.newInstance("acctId");

    //channelId-Channel Map
    private  Map<String, Channel> channelMap = new ConcurrentHashMap<>();
    //已登录channel
    private  Map<String, Channel> acctChannelMap = new ConcurrentHashMap<>();


    private ServerListener listener;

    public Map<String, Channel> getChannels(){
        return channelMap;
    }
    public Channel getChannel(String acctId){
        return acctChannelMap.get(acctId);
    }


    public ServerHandler(ServerListener listener){
        this.listener=listener;
    }
    /**
     * 客户端连接会触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String channelId=ctx.channel().id().toString();
        channelMap.put(channelId,ctx.channel());
        log.info("Channel active......{}",channelId);
        this.channelMap.put(channelId,ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String channelId=ctx.channel().id().toString();
        log.info("Channel Inactive......{}",channelId);
        if(this.channelMap.containsKey(channelId)){
            this.channelMap.remove(channelId);
        }
        String acctId=ctx.channel().attr(ATTRI_ACCT_ID).get();
        if(StringUtils.isNotEmpty(channelId)){
            this.acctChannelMap.remove(channelId);
            this.listener.onConnect(acctId,false);
        }
    }

    /**
     * 客户端发消息会触发
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("服务端收到消息: {}", msg.toString());
        Message request = JSON.parseObject(msg.toString(), Message.class);;
        if(request.getType() == Enums.MSG_TYPE.LOGIN){
            String acctId = request.getAcctId();
            ctx.channel().attr(ATTRI_LOGIN).set(true);
            ctx.channel().attr(ATTRI_ACCT_ID).set(acctId);

            acctChannelMap.put(acctId,ctx.channel());
            listener.onConnect(acctId,true);
        }else if(request.getType()!= Enums.MSG_TYPE.PING){
            //通用处理
            Message response=listener.onRequest(request);
            String resMsg= JSON.toJSONString(response);
            ctx.channel().writeAndFlush(resMsg);
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

    public void reset(){
        channelMap.clear();
        acctChannelMap.clear();
    }
}