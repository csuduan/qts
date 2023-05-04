package org.mts.admin.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring辅助工具类型
 *
 */
@Component
public final class SpringUtils implements ApplicationContextAware {
    private static ApplicationContext context = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        if(context ==null)
            context = applicationContext;
    }
    public static ApplicationContext getContext(){
        return context;
    }

    public static void autowire(Object bean) {
        context.getAutowireCapableBeanFactory().autowireBean(bean);
    }

    public static <T> T getBean(String beanName,Class<T> clzz) {
        return context.getBean(beanName,clzz);
    }

    public static <T> T getBean(Class<T> clzz) {
        return context.getBean(clzz);
    }
}
