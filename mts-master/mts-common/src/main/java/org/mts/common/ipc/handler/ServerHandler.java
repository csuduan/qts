package org.mts.common.ipc.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private  Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public Map<String, Channel> getChannels(){
        return channelMap;
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
        log.info("服务器收到消息: {}", msg.toString());
        //ctx.write("你也好哦");
        //ctx.flush();
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