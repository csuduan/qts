package org.mts.common.ipc.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.mts.common.ipc.handler.ClientHandler;
import org.mts.common.ipc.handler.CustomHandler;

import java.net.InetSocketAddress;

@Slf4j
public class TcpClient {
    private EventLoopGroup group = new NioEventLoopGroup();
    private boolean enable=true;
    private Channel channel;
    private ClientHandler clientHandler;
    private String name;
    private String address;
    private CustomHandler customHandler;

    public TcpClient(String name, String address, CustomHandler customHandler){
        this.name=name;
        this.address=address;
        this.customHandler=customHandler;
        log.info("create client[{}]  {}",name,address);
    }

    public void close(){
        if(this.enable){
            this.enable=false;
            group.shutdownGracefully();
            log.info("close cliet[{}]",name);
        }
    }
    public boolean send(String msg){
        boolean ret=false;
        if(!enable){
            log.error("client[{}] 已关闭",name);
        }
        if(channel!=null && channel.isActive()){
            channel.writeAndFlush(msg);
            log.info("{} send msg:{}",name,msg);
            ret =true;
        }else {
            log.error("client[{}]  未连接",name);
        }
        return ret;

    }

    public void connect() {
        if(!enable){
            log.error("client[{}] 已关闭",name);
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
            log.error("client[{}] connecting...",name);
            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(host,port)).sync();
            channel=channelFuture.channel();
            channel.closeFuture().sync();
        }catch (Exception ex){
            log.error("client[{}] error:{}",name,ex.getMessage());
        }finally {

        }
    }

    public boolean isConnected(){
        return this.clientHandler.isConnected();
    }
}
