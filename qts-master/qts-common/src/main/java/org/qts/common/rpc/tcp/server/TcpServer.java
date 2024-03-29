package org.qts.common.rpc.tcp.server;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.qts.common.entity.Message;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
public class TcpServer {

    private boolean started=false;
    private ServerHandler serverHandler;
    private int port;
    private MsgHandler msgHandler;

    public TcpServer(int port, MsgHandler msgHandler){
        this.port=port;
        this.msgHandler =msgHandler;
    }

    public void start(){
        if(started==true){
            log.error("already stated!!!");
            return;
        }

//        Thread thread=new Thread(()->this.run());
//        thread.start();
        this.run();
    }
    private void run() {
        if(started==true){
            log.error("already stated!!!");
            return;
        }
        serverHandler=new ServerHandler(this.msgHandler);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // nio服务的启动类
            ServerBootstrap sbs = new ServerBootstrap();
            // 配置nio服务参数
            sbs.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // 说明一个新的Channel如何接收进来的连接
                    .option(ChannelOption.SO_BACKLOG, 128) // tcp最大缓存链接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) //保持连接
                    .handler(new LoggingHandler(LogLevel.INFO)) // 打印日志级别
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                           //socketChannel.pipeline().addLast(new ReadTimeoutHandler(5));
                            log.info("new socket !");
                            socketChannel.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            socketChannel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                            socketChannel.pipeline().addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
                            socketChannel.pipeline().addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
                            // 处理接收到的请求
                            socketChannel.pipeline().addLast(serverHandler); // 这里相当于过滤器，可以配置多个
                        }
                    });

            this.started=true;
            log.info("start tcpServer,port:{}",port);
            // 绑定端口，开始接受链接
            ChannelFuture cf = sbs.bind(port).sync();
            cf.channel().closeFuture().sync();
            log.info("close TcpServer,port:{}",port);
        } catch (Exception ex){
            log.error("start tcpServer error,port:{}",port,ex);
        }
        finally{
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    //服务端推送
    public void push(Message message){
        serverHandler.push(message);
    }
}
