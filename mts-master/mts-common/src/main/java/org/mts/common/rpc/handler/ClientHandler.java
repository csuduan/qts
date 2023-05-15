package org.mts.common.rpc.handler;

import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.mts.common.model.Enums;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.future.SyncWriteFuture;
import org.mts.common.rpc.future.SyncWriteMap;
import org.mts.common.rpc.listener.ClientListener;
import org.springframework.util.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Data
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private String id;
    private boolean connected=false;
    private ClientListener eventListener;

    public ExecutorService threadPool = Executors.newCachedThreadPool();

    public ClientHandler(String id, ClientListener eventListener){
        super();
        this.id =id;
        this.eventListener = eventListener;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connected=true;
        log.info("{} Channel connected......", id);
        threadPool.submit(()->{
            //发送PING
            Message pingMsg=new Message(Enums.MSG_TYPE.PING,null);
            String ping=JSON.toJSONString(pingMsg);
            ctx.channel().writeAndFlush(ping);
            //状态处理
            eventListener.onStatus(id,true);
        });

        //ctx.channel().writeAndFlush("ping");
        //ctx.writeAndFlush("hello world ,hello word");
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connected=false;
        log.info("{} Channel disconnected......", id);
        threadPool.submit(()->{
            eventListener.onStatus(id,false);
        });
        //ctx.channel().writeAndFlush("ping");
        //ctx.writeAndFlush("hello world ,hello word");
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到消息: {}", msg.toString());
        Message response = JSON.parseObject(msg.toString(), Message.class);
        response.setSuccess(true);
        if(StringUtils.hasLength(response.getRequestId())) {
            String requestId = response.getRequestId();
            SyncWriteFuture future = (SyncWriteFuture) SyncWriteMap.syncKey.get(requestId);
            if (future != null) {
                future.setResponse(response);
            }else{
                log.error("匹配不到请求回话,requestId:{}",requestId);
            }
        }else {
            //推送消息
            eventListener.onMessage(response);
        }
    }

    /**
     * 发生异常触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("{} channel exception", id,cause);
    }
}
