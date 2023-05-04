package org.mts.admin.ipc.uds;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.WsSession;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ChannelHandler.Sharable
public class UdsServerHandler extends ChannelInboundHandlerAdapter {
    //id-channel
    private final ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();
    //id-account
    private final ConcurrentHashMap<String, String> accountMap = new ConcurrentHashMap<>();

    private UdsServer udsServer;

    public UdsServerHandler(UdsServer udsServer){
     this.udsServer=udsServer;
    }
    /**
     * 客户端连接会触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelMap.put(ctx.channel().id().toString(),ctx.channel());
        log.info("Channel active......");
        UdsSession session=new UdsSession();
        session.setId(ctx.channel().id().toString());
        session.setChannel(ctx.channel());
        this.udsServer.getSessionMap().put(ctx.channel().id().toString(),session);
        //ctx.channel().writeAndFlush("ping");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelMap.put(ctx.channel().id().toString(),ctx.channel());
        log.info("Channel Inactive......");
        if(this.udsServer.getSessionMap().containsKey(ctx.channel().id().toString())){
            this.udsServer.getSessionMap().remove(ctx.channel().id().toString());
        }
    }


    /**
     * 客户端发消息会触发
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("服务器收到消息: {}", msg.toString());
        ctx.write("{}");
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