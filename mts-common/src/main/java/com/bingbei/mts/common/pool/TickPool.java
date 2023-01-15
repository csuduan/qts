package com.bingbei.mts.common.pool;

import com.bingbei.mts.common.entity.Tick;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.stereotype.Component;

@Component
public class TickPool{
    private GenericObjectPool<Tick> objectPool;
    public TickPool() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(5);
        objectPool=new GenericObjectPool<>(new TickFactory(),config);
    }
    public Tick borrowTick() throws  Exception{
        return objectPool.borrowObject();
    }
    public void returnTick(Tick tick){
        objectPool.returnObject(tick);
    }


    private class TickFactory extends BasePooledObjectFactory<Tick> {
        @Override
        public Tick create() throws Exception {
            return new Tick();
        }

        @Override
        public PooledObject<Tick> wrap(Tick tick) {
            //包装实际对象
            return new DefaultPooledObject<>(tick);
        }

    }

}
