package org.mts.common.rpc.uds;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.future.SyncWrite;
import org.mts.common.rpc.handler.ClientHandler;
import org.mts.common.rpc.listener.ClientListener;

import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class UdsClient {
    private EventLoopGroup group = new EpollEventLoopGroup();
    private boolean enable=true;
    private Channel channel;
    private ClientHandler clientHandler;
    private String name;
    private ClientListener customHandler;
    private SyncWrite writer = new SyncWrite();


    public UdsClient(String name, ClientListener customHandler){
        this.name=name;
        this.customHandler=customHandler;
        log.info("create client[{}] ",name);
    }

    public void start(){
        log.info("start client[{}]",name);
        Thread thread=new Thread(()->{
            while (enable){
                try {
                    if(!this.isConnected()){
                        this.connect();
                    }
                    Thread.sleep(10000);
                }catch (Exception ex){

                }
            }
        });
        thread.setName("udsClient");
        thread.start();
    }

    public void close(){
        if(this.enable){
            this.enable=false;
            group.shutdownGracefully();
            log.info("close cliet[{}]",name);
        }
    }

    public Message request(Message req){
        try {
            return writer.writeAndSync(channel, req, 5000);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Synchronized
    public void connect() {
        if(!enable){
            log.error("client[{}] 已关闭",name);
            return;
        }
        if(this.isConnected()){
            log.warn("client[{}] 已经连接",name);
            return;
        }

        String sockPath="/tmp/ipc/"+name+".sock";
        if(!Files.exists(Paths.get(sockPath))){
            //log.warn("sock[{}] not exist",sockPath);
            return;
        }

        try {
            Bootstrap bootstrap;
            bootstrap = new Bootstrap();
            clientHandler=new ClientHandler(name,customHandler);
            bootstrap.group(group)
                    .channel(EpollDomainSocketChannel.class)
                    //.option(ChannelOption.TCP_NODELAY,true)
                    //.option(ChannelOption.SO_KEEPALIVE,true)
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
            log.error("client[{}] connecting...",name);
            SocketAddress sock = new DomainSocketAddress(sockPath);
            ChannelFuture channelFuture = bootstrap.connect(sock).sync();
            channel=channelFuture.channel();
            channel.closeFuture().sync();
        }catch (Exception ex){
            log.error("client[{}] error:{}",name,ex.getMessage());
        }finally {

        }
    }

    public boolean isConnected(){
        return this.channel!=null && this.clientHandler.isConnected();
    }

}
