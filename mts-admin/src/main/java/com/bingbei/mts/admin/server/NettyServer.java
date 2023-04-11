package com.bingbei.mts.admin.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyServer {
    @Autowired
    private NettyServerHandler nettyServerHandler;
    private int port=8090;
    //@PostConstruct
    public void start() {
        // 用来接收进来的连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 用来处理已经被接收的连接，一旦bossGroup接收到连接，就会把连接信息注册到workerGroup上
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
                            socketChannel.pipeline().addLast(nettyServerHandler); // 这里相当于过滤器，可以配置多个
                        }
                    });

            System.err.println("server 开启--------------");
            log.info("启动NettyServer,port:{}",port);
            // 绑定端口，开始接受链接
            ChannelFuture cf = sbs.bind(port).sync();
            cf.channel().closeFuture().sync();
            log.info("关闭NettyServer,port:{}",port);
        } catch (Exception ex){
            log.error("启动Netty服务异常",ex);
        }
        finally{
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
