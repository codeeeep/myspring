package com.cy.service;

/**
 * @author codeep
 * @date 2024/3/15
 * @description:
 */
public interface UserService {

    /**
     * 查询所有用户
     */
    void queryAllUsers();

    /**
     * 打印注入的 orderService
     */
    void methodWithOrders();

    /**
     * 打印 Aware 回调获取的 beanName
     */
    void printBeanName();

}
