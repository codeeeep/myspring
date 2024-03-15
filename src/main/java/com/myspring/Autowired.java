package com.myspring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: 依赖注入的注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Autowired {

    /**
     * 声明该注解标注的依赖是否需要一定存在于Spring容器中, 默认必须存在
     * true为必须存在，如果不存在的话就抛出NoSuchBeanDefinitionException异常
     * false不要求必须存在，如果不存在也不抛出异常（一般不建议设置，可能会引发线上事故）
     */
    boolean required() default true;

}
