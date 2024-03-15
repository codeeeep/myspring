package com.myspring;

/**
 * @author codeep
 * @date 2024/3/15
 * @description: 后置处理器
 */
public interface BeanPostProcessor {

    /**
     * Bean 的初始化之前触发
     *
     * @param bean     进行初始化的 Bean
     * @param beanName Bean 的名称
     * @return 处理后的 Bean
     */
    Object postProcessBeforeInitialization(Object bean, String beanName);

    /**
     * Bean 的初始化之后触发
     *
     * @param bean     进行初始化的 Bean
     * @param beanName Bean 的名称
     * @return 处理后的 Bean
     */
    Object postProcessAfterInitialization(Object bean, String beanName);

}
