package com.cy.service;

import com.myspring.Autowired;
import com.myspring.BeanNameAware;
import com.myspring.Component;
import com.myspring.Scope;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: 客户端定义的 Service
 */
@Component("userService")
@Scope("singleton")
public class UserServiceImpl implements UserService, BeanNameAware {

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
    public void methodWithOrders() {
        System.out.println(orderService);
    }

    @Override
    public void printBeanName() {
        System.out.println(beanName);
    }


}
