package org.mts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.admin.dao.SysMapper;
import org.mts.admin.entity.po.RouterPo;
import org.mts.admin.entity.sys.Router;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SysService {
    @Autowired
    private SysMapper sysMapper;


    public List<Router> getSysRouters(String user){
        List<RouterPo> routerPos= sysMapper.getRouters();
        Map<String,Router> routerMap=new HashMap();
        routerPos.forEach(po->routerMap.put(po.getName(),buildRouter(po)));
        List<Router> rootRouters=new ArrayList<>();
        routerMap.values().forEach(router->{
            if(StringUtils.hasLength(router.getParent())){
                Router parent=routerMap.get(router.getParent());
                if(parent!=null){
                    if(parent.getChildren()==null)
                        parent.setChildren(new ArrayList<>());
                    parent.getChildren().add(router);
                }else{
                    log.error("router[{}]can not find parent router[{}]",router.getName(),router.getParent());
                }
            }else{
                //根节点
                rootRouters.add(router);
            }
        });
        return rootRouters;
    }

    private Router buildRouter(RouterPo po){
        Router router=new Router();
        BeanUtils.copyProperties(po,router);
        BeanUtils.copyProperties(po,router.getMeta());
        return router;
    }
}
