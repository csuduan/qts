package org.mts.common.ipc.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private String name;
    private boolean connected=false;
    private CustomHandler customHandler;
    public ClientHandler(String name,CustomHandler customHandler){
        super();
        this.name=name;
        this.customHandler=customHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connected=true;
        log.info("{} Channel connected......",name);
        customHandler.onStatus(true);
        //ctx.channel().writeAndFlush("ping");
        //ctx.writeAndFlush("hello world ,hello word");
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connected=false;
        log.info("{} Channel disconnected......",name);
        customHandler.onStatus(false);
        //ctx.channel().writeAndFlush("ping");
        //ctx.writeAndFlush("hello world ,hello word");
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到消息: {}", msg.toString());
        customHandler.onData(msg);
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
