package com.cy.service;

import com.myspring.Autowired;
import com.myspring.Component;
import com.myspring.Scope;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: 客户端定义的 Service
 */
@Component("userService")
@Scope("singleton")
public class UserServiceImpl implements UserService {

    @Autowired
    private OrderService orderService;

    @Override
    public void queryAllUsers() {
        System.out.println("调用了方法：查找所有的用户");
    }

    @Override
    public void methodWithOrders() {
        System.out.println(orderService);
    }

}
