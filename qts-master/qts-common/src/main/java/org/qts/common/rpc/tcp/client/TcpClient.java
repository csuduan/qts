package org.qts.common.rpc.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.qts.common.entity.Message;
import org.qts.common.rpc.future.SyncWrite;

import java.net.InetSocketAddress;

@Slf4j
public class TcpClient {
    private boolean started = false;
    private  Bootstrap bootstrap;
    private Channel channel;
    private ClientHandler clientHandler;
    private String name;
    private String host="127.0.0.1";
    private Integer port;
    private MsgHandler customHandler;
    private SyncWrite writer = new SyncWrite();


    public TcpClient(String name, Integer port, MsgHandler customHandler){
        this.name=name;
        this.port = port;
        this.customHandler=customHandler;
        log.info("create client[{}]  {}",name,host+":"+port);
    }
    public void start(){
        if(this.started)
            return;

        log.info("start client[{}]",name);
        this.started = true;
        Thread thread=new Thread(()->{
            while (started){
                try {
                    if(!this.isConnected()){
                        this.connect();
                    }
                    Thread.sleep(5000);
                }catch (Exception ex){

                }
            }
        });
        thread.setName("tcpClient");
        thread.start();
    }

    public void stop(){
        if(this.started){
            this.started=false;
            if(bootstrap!=null){
                bootstrap.group().shutdownGracefully();
                bootstrap = null;
            }
            log.info("close cliet[{}]",name);
        }
    }

    public Message request(Message req){
        try {
            log.info("cliet[{}] request:{}",name,req);
            return writer.writeAndSync(channel, req, 5000);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Synchronized
    public void connect() {
        if(!started){
            log.error("client[{}] 已关闭",name);
            return;
        }
        if(this.isConnected()){
            log.warn("client[{}] 已经连接",name);
            return;
        }

        try {
            this.bootstrap = new Bootstrap();
            this.clientHandler=new ClientHandler(name,customHandler);
            bootstrap.group(new NioEventLoopGroup())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .option(ChannelOption.SO_KEEPALIVE,true)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        protected void initChannel(NioSocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            socketChannel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                            socketChannel.pipeline().addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
                            socketChannel.pipeline().addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
                            // 处理接收到的请求
                            socketChannel.pipeline().addLast(clientHandler); // 这里相当于过滤器，可以配置多个
                        }
                    });
            log.info("client[{}] connecting...",name);
            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(host,port)).sync();
            channel=channelFuture.channel();
            channel.closeFuture().sync();
            log.info("client[{}] disconnected",name);
        }catch (Exception ex){
            log.error("client[{}] error:{}",name,ex.getMessage());
        }finally {
        }
    }
    public boolean isConnected(){
        return this.clientHandler!=null && this.clientHandler.isConnected();
    }
}
