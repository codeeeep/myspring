package com.cy.service;

import com.myspring.After;
import com.myspring.Aspect;
import com.myspring.Before;
import com.myspring.Component;

/**
 * @author codeep
 * @date 2024/3/16
 * @description: 用于实现日志切面, execute 表达式用 beanName.methodName 代替
 */
@Component("log")
@Aspect
public class Log {

    @Before("userService.methodWithOrders")
    public void logBefore() {
        System.out.println("【debug】这是方法执行前的日志输出。");
    }

    @After("orderService.methodWithUsers")
    public void logAfter() {
        System.out.println("【debug】这是方法执行后的日志输出。");
    }

}
