package com.cy;

import com.myspring.ComponentScan;
import com.myspring.EnableAspectAutoProxy;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: 客户端模拟传入的 Spring 配置信息
 */

@ComponentScan("com.cy.service")
@EnableAspectAutoProxy
public class AppConfig {

}
