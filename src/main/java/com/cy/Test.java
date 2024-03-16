package com.cy;

import com.cy.service.OrderService;
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
        // 1.1 测试单例和多例
        // UserService userService2 = (UserService) applicationContext.getBean("userService");
        // UserService userService3 = (UserService) applicationContext.getBean("userService");
        // System.out.println(userService1 == userService2 && userService2 == userService3);
        // 1.2 测试启动时扫描是否成功
        // userService1.queryAllUsers();
        // 2. 测试依赖注入是否成功
        // userService1.methodWithOrders();
        // 3. 测试 Aware 回调是否成功
        // userService1.printBeanName();
        // 4. 测试 Bean 的初始化接口是否成功
        // 5. 测试后置处理器是否成功
        // userService1.printBeanName();
        // 6.1 测试循环依赖
        OrderService orderService1 = (OrderService) applicationContext.getBean("orderService");
        System.out.println(orderService1);
        userService1.methodWithOrders();
        System.out.println(userService1);
        orderService1.methodWithUsers();

    }

}
