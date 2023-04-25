package com.bingbei.mts.common.ipc.uds;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    private String name;
    private boolean connected=false;
    public NettyClientHandler(String name){
        super();
        this.name=name;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connected=true;
        log.info("{} Channel connected......",name);
        //ctx.channel().writeAndFlush("ping");
        //ctx.writeAndFlush("hello world ,hello word");
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connected=false;
        log.info("{} Channel disconnected......",name);
        //ctx.channel().writeAndFlush("ping");
        //ctx.writeAndFlush("hello world ,hello word");
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到消息: {}", msg.toString());
    }

    /**
     * 发生异常触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("{} channel exception",name,cause);
        connected=false;
        ctx.close();
    }
}
