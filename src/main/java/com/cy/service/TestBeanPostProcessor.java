package com.cy.service;

import com.myspring.BeanPostProcessor;
import com.myspring.Component;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: 客户端自定义给 Bean 初始化之前和之后做的操作
 */
@Component("testBeanPostProcessor")
public class TestBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if ("userService".equals(beanName)) {
            ((UserServiceImpl) bean).setName("初始化之前改名的userService");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if ("userService".equals(beanName)) {
            ((UserServiceImpl) bean).setName("初始化之后改名的userService");
        }
        return bean;
    }
}
