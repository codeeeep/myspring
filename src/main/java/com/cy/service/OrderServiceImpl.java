package com.cy.service;

import com.myspring.Autowired;
import com.myspring.Component;

/**
 * @author codeep
 * @date 2024/3/15
 * @description:
 */
@Component("orderService")
public class OrderServiceImpl implements OrderService{

    @Autowired
    private UserService userService;

    @Override
    public void methodWithUsers() {
        System.out.println(userService);
    }
}
