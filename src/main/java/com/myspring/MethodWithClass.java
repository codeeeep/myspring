package com.myspring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * @author codeep
 * @date 2024/3/16
 * @description: 封装通知方法及其所属的类对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MethodWithClass {

    /**
     * 切面类的类对象
     */
    private Class clazz;

    /**
     * 通知方法
     */
    private Method method;

}
