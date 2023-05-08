package org.mts.agent.server;

import lombok.extern.slf4j.Slf4j;
import org.mts.common.ipc.tcp.TcpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class AdminServer {

    @Autowired
    private TcpServer tcpServer;
    @Value("${server.port}")
    private  int port;

    @PostConstruct
    public void start(){
        tcpServer.start(port);
    }
}
