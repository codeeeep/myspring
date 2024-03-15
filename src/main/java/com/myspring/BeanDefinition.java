package com.myspring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: Bean 定义类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeanDefinition {

    /**
     * Bean 的类型
     */
    private Class clazz;

    /**
     * Bean 的作用域
     */
    private String Scope;

}
