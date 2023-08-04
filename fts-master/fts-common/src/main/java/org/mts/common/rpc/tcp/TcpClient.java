package org.mts.common.rpc.tcp;

import com.alibaba.fastjson.JSON;
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
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.future.SyncWrite;
import org.mts.common.rpc.handler.ClientHandler;
import org.mts.common.rpc.listener.ClientListener;

import java.net.InetSocketAddress;

@Slf4j
public class TcpClient {
    private boolean enable=true;
    private EventLoopGroup group = new NioEventLoopGroup();
    private Channel channel;
    private ClientHandler clientHandler;
    private String name;
    private String address;
    private ClientListener customHandler;
    private SyncWrite writer = new SyncWrite();


    public TcpClient(String name, String address, ClientListener customHandler){
        this.name=name;
        this.address=address;
        this.customHandler=customHandler;
        log.info("create client[{}]  {}",name,address);
    }
    public void start(){
        log.info("start client[{}]",name);
        Thread thread=new Thread(()->{
            while (enable){
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

    public void close(){
        if(this.enable){
            this.enable=false;
            group.shutdownGracefully();
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
        if(!enable){
            log.error("client[{}] 已关闭",name);
            return;
        }
        if(this.isConnected()){
            log.warn("client[{}] 已经连接",name);
            return;
        }

        try {
            String[] tmp=this.address.split(":");
            String host=tmp[0];
            int port=Integer.parseInt(tmp[1]);

            Bootstrap bootstrap;
            bootstrap = new Bootstrap();
            clientHandler=new ClientHandler(name,customHandler);
            bootstrap.group(group)
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
