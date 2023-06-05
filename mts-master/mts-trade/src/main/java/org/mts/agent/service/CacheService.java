package org.mts.agent.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.mts.agent.dao.CacheMapper;
import org.mts.agent.model.CachePo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CacheService {
    @Autowired
    private CacheMapper cacheMapper;

    public boolean save(String type,String key,Object obj){
        if(!StringUtils.hasLength(type) || !StringUtils.hasLength(key)){
            log.error("type or key is empty");
            return false;
        }
        String json=JSON.toJSONString(obj);
        CachePo cachePo=new CachePo();
        cachePo.setType(type);
        cachePo.setKey(key);
        cachePo.setValue(json);

        int count=cacheMapper.count(type,key);
        if(count==0)
            cacheMapper.insert(cachePo);
        else
            cacheMapper.update(cachePo);
        return true;
    }

    public <T> List<T> getCaches(String type, Class<T> clazz){
        List<CachePo> cachePos=cacheMapper.getCaches(type);
        List<T> list=new ArrayList<>();
        cachePos.forEach(x->{
            T data= JSON.parseObject(x.getValue(),clazz);
            list.add(data);
        });
        return list;
    }

    public <T> T getCache(String type,String key ,Class<T> clazz){
        CachePo cachePo=cacheMapper.getCacheByKey(type,key);
        T data= JSON.parseObject(cachePo.getValue(),clazz);
        return data;
    }
}
