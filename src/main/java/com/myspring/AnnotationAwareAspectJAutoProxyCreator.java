package com.myspring;

/**
 * @author codeep
 * @date 2024/3/16
 * @description:
 */
public class AnnotationAwareAspectJAutoProxyCreator implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
