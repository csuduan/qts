package org.mts.admin.ipc.uds;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class UdsServer {
    private Map<String, UdsSession> sessionMap=new HashMap<>();

    private String uname;

    public Map<String, UdsSession> getSessionMap(){
        return this.sessionMap;
    }

    //@PostConstruct
    public void start(String uname) {
        this.uname=uname;

        Thread thread=new Thread(()->this.run());
        thread.start();
    }

    public void run(){
        SocketAddress sock = new DomainSocketAddress("/tmp/"+uname+".sock");
        // 用来接收进来的连接
        EpollEventLoopGroup bossGroup = new EpollEventLoopGroup();
        // 用来处理已经被接收的连接，一旦bossGroup接收到连接，就会把连接信息注册到workerGroup上
        EpollEventLoopGroup workerGroup = new EpollEventLoopGroup();
        UdsServerHandler udsServerHandler=new UdsServerHandler(this);
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
                            // marshalling 序列化对象的解码
//                      socketChannel.pipeline().addLast(MarshallingCodefactory.buildDecoder());
                            // marshalling 序列化对象的编码
//                      socketChannel.pipeline().addLast(MarshallingCodefactory.buildEncoder());
                            // 网络超时时间
//                      socketChannel.pipeline().addLast(new ReadTimeoutHandler(5));
                            socketChannel.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            socketChannel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                            socketChannel.pipeline().addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
                            socketChannel.pipeline().addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
                            // 处理接收到的请求
                            socketChannel.pipeline().addLast(udsServerHandler); // 这里相当于过滤器，可以配置多个
                        }
                    });
            log.info("启动Uds Server,uname:{}",uname);
            // 绑定端口，开始接受链接
            ChannelFuture cf = sbs.bind(sock).sync();
            cf.channel().closeFuture().sync();
            log.info("关闭Uds Server,uname:{}",uname);
        } catch (Exception ex){
            log.error("启动关闭Uds Server异常",ex);
        }
        finally{
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
