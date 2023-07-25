package org.mts.admin.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.mts.admin.entity.WsMessage;
import org.mts.common.model.event.MessageEvent;
import org.mts.common.model.rpc.Message;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.HashSet;
import java.util.Set;

@Service
@ServerEndpoint("/ws/{type}")
@Slf4j
public class WebSocketService {
    private Session session;
    private static Set<Session> sessions=new HashSet<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("type")String type){
        this.session=session;
        log.info("onOpen {}",session.getId());
        sessions.add(session);
    }
    @OnClose
    public void onClose(){
        log.info("onClose {}",session.getId());
        sessions.remove(this.session);
    }
    @OnMessage
    public void onMessage(Session session, @PathParam("type")String type,String msg){
        log.info("recv:{}",msg);
    }
    @OnError
    public void onError(Session session ,Throwable throwable){

    }
    public void push(Message message){
        String msg= JSON.toJSONString(message);
        sessions.forEach(session -> {
            try {
                if(session.isOpen())
                    session.getBasicRemote().sendText(msg);
            }catch (Exception ex){
                log.error("push to session[{}]error!",session.getId(),ex);
            }
        });
    }

    @EventListener(MessageEvent.class)
    public void eventHandler(MessageEvent messageEvent){
        this.push((Message) messageEvent.getSource());
    }

}
