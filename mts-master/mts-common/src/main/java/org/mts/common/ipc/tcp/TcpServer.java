package org.mts.common.ipc.tcp;

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
import org.mts.common.ipc.handler.ServerHandler;
import org.mts.common.model.Message;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@Lazy
public class TcpServer {

    private boolean started=false;
    private ServerHandler serverHandler=new ServerHandler();
    private int port;

    public void start(int port){
        if(started==true){
            log.error("already stated!!!");
            return;
        }
        this.port=port;

        Thread thread=new Thread(()->this.run());
        thread.start();
    }
    private void run() {
        if(started==true){
            log.error("already stated!!!");
            return;
        }
        serverHandler.reset();
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

                            // marshalling 序列化对象的解码
//                      socketChannel.pipeline().addLast(MarshallingCodefactory.buildDecoder());
                            // marshalling 序列化对象的编码
//                      socketChannel.pipeline().addLast(MarshallingCodefactory.buildEncoder());
                            // 网络超时时间
//                      socketChannel.pipeline().addLast(new ReadTimeoutHandler(5));
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
            log.info("启动TcpServer,port:{}",port);
            // 绑定端口，开始接受链接
            ChannelFuture cf = sbs.bind(port).sync();
            cf.channel().closeFuture().sync();
            log.info("关闭TcpServer,port:{}",port);
        } catch (Exception ex){
            log.error("启动TcpServer异常,port:{}",port,ex);
        }
        finally{
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void push(Message message){
        if(StringUtils.hasLength(message.getRid())){
            //推动个指定客户端
        }else{
            //广播
            serverHandler.getChannels().values().forEach(channel -> channel.writeAndFlush(message));
        }
    }
}
