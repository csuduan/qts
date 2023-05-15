package org.mts.common.rpc.uds;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.mts.common.rpc.listener.ClientListener;
import org.mts.common.rpc.handler.ServerHandler;
import org.mts.common.rpc.listener.ServerListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.SocketAddress;

@Slf4j
@Component
@Lazy
public class UdsServer {

    private String uname;
    private boolean started;
    private ServerHandler serverHandler;

    public void start(String uname, ServerListener customHandler) {
        if(started==true){
            log.error("already stated!!!");
            return;
        }

        this.uname=uname;
        serverHandler=new ServerHandler(customHandler);
        Thread thread=new Thread(()->this.run());
        thread.start();
    }

    public void run(){
        SocketAddress sock = new DomainSocketAddress("/tmp/ipc/"+uname+".sock");
        // 用来接收进来的连接
        EpollEventLoopGroup bossGroup = new EpollEventLoopGroup();
        // 用来处理已经被接收的连接，一旦bossGroup接收到连接，就会把连接信息注册到workerGroup上
        EpollEventLoopGroup workerGroup = new EpollEventLoopGroup();
        try {
            //服务的启动类
            ServerBootstrap sbs = new ServerBootstrap();
            // 配置nio服务参数
            sbs.group(bossGroup, workerGroup)
                    .channel(EpollServerDomainSocketChannel.class) // 说明一个新的Channel如何接收进来的连接
                    .option(ChannelOption.SO_BACKLOG, 128) // 最大缓存链接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) //保持连接
                    .handler(new LoggingHandler(LogLevel.INFO)) // 打印日志级别
                    .childHandler(new ChannelInitializer<EpollDomainSocketChannel>() {
                        @Override
                        protected void initChannel(EpollDomainSocketChannel socketChannel) throws Exception {
                            // 网络超时时间
                            //socketChannel.pipeline().addLast(new ReadTimeoutHandler(5));
                            socketChannel.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            socketChannel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                            socketChannel.pipeline().addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
                            socketChannel.pipeline().addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
                            // 处理接收到的请求
                            socketChannel.pipeline().addLast(serverHandler); // 这里相当于过滤器，可以配置多个
                        }
                    });
            this.started=true;
            log.info("启动Uds Server,uname:{}",uname);
            // 绑定端口，开始接受链接
            ChannelFuture cf = sbs.bind(sock).sync();
            cf.channel().closeFuture().sync();
            log.info("关闭Uds Server,uname:{}",uname);
        } catch (Exception ex){
            log.error("启动Uds Server异常,uname:{}",uname,ex);
        }
        finally{
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
