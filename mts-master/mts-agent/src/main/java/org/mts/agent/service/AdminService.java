package org.mts.agent.service;

import org.mts.agent.server.AdminServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    @Autowired
    private AdminServer adminServer;

    public boolean pushData(){
        return true;
    }

    public boolean connectAdmin(){
        return true;
    }

}
