package org.mts.admin.ipc.uds;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

@Slf4j
public class NettyClient {
    private EventLoopGroup group = new EpollEventLoopGroup();
    private String socketPath;
    private String unName;
    private boolean enable=true;
    private Channel channel;
    private NettyClientHandler clientHandler;

    public NettyClient(String unName){
        this.unName=unName;
        log.info("create nettyCleint {}",unName);
        this.socketPath="/tmp/sock/"+unName;
        Thread thread=new Thread(()->{
            while (enable){
                this.connect();
                clientHandler.setConnected(false);
                log.error("{}连接断开，稍后将自动重试",unName);
                try {
                    Thread.sleep(10000);
                }catch (Exception ex){

                }
            }
        });
        thread.start();
    }
    public void close(){
        this.enable=false;
        group.shutdownGracefully();
        log.info("close nettyCliet {}",unName);
    }
    public boolean send(String msg){
        boolean ret=false;
        if(!enable){
            log.error("nettyClient {} 已关闭",unName);
        }
        if(channel!=null && channel.isActive()){
            channel.writeAndFlush(msg);
            log.info("{} send msg:{}",unName,msg);
            ret =true;
        }else {
            log.error("nettyClient {} 未连接",unName);
        }
        return ret;

    }

    @PostConstruct
    public void connect() {
        try {
            Bootstrap bootstrap;
            bootstrap = new Bootstrap();
            clientHandler=new NettyClientHandler(unName);
            bootstrap.group(group)
                    .remoteAddress(new DomainSocketAddress(socketPath))
                    //长链接
                    .channel(EpollDomainSocketChannel.class)
                    .handler(new ChannelInitializer<EpollDomainSocketChannel>() {
                        protected void initChannel(EpollDomainSocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            socketChannel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                            socketChannel.pipeline().addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
                            socketChannel.pipeline().addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
                            // 处理接收到的请求
                            socketChannel.pipeline().addLast(clientHandler); // 这里相当于过滤器，可以配置多个
                        }
                    });

            ChannelFuture channelFuture = bootstrap.connect().sync();
            channel=channelFuture.channel();
            channel.closeFuture().sync();
        }catch (Exception ex){
            log.error("{} netty client error",unName,ex.getMessage());
        }finally {

        }
    }

    public boolean isConnected(){
        return this.clientHandler.isConnected();
    }
}
