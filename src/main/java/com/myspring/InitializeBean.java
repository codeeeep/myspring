package com.myspring;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: Bean 的初始化接口
 */
public interface InitializeBean {

    void afterPropertiesSet() throws Exception;

}
