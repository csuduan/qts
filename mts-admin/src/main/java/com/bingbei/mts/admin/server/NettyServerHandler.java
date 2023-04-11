package com.bingbei.mts.admin.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    //id-channel
    private final ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();
    //id-account
    private final ConcurrentHashMap<String, String> accountMap = new ConcurrentHashMap<>();
    /**
     * 客户端连接会触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelMap.put(ctx.channel().id().toString(),ctx.channel());
        log.info("Channel active......");
        ctx.channel().writeAndFlush("ping");
    }

    /**
     * 客户端发消息会触发
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("服务器收到消息: {}", msg.toString());
        ctx.write("你也好哦");
        ctx.flush();
    }

    /**
     * 发生异常触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}