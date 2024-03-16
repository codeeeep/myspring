package com.cy.service;

import com.myspring.*;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: 客户端定义的 Service
 */
@Component("userService")
@Scope("singleton")
public class UserServiceImpl implements UserService, BeanNameAware, InitializeBean {

    @Autowired
    private OrderService orderService;

    private String beanName;

    @Override
    public void setName(String name) {
        this.beanName = name;
    }

    @Override
    public void queryAllUsers() {
        System.out.println("调用了方法：查找所有的用户");
    }

    @Override
    public OrderService methodWithOrders() {
        // System.out.println(orderService);
        return orderService;
    }

    @Override
    public void printBeanName() {
        System.out.println(beanName);
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println(beanName + "被初始化了！");
    }
}
