package com.cy;

import com.cy.service.UserService;
import com.myspring.AnnotationConfigApplicationContext;

/**
 * @author codeep
 * @date 2024/3/15 10:45
 * @description: 客户端调用 Spring 的代码，目前只实现用配置类的方式，而非 XML 解析的方式
 */
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService userService1 = (UserService) applicationContext.getBean("userService");
        UserService userService2 = (UserService) applicationContext.getBean("userService");
        UserService userService3 = (UserService) applicationContext.getBean("userService");
        System.out.println(userService1 == userService2 && userService2 == userService3);
        userService1.queryAllUsers();

    }

}
