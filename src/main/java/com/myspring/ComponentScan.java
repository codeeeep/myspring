package com.myspring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: 包扫描注解，作用于类上
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentScan {

    /**
     * 扫描包名，只支持同级包文件，不支持包中包文件
     */
    String value();

}
