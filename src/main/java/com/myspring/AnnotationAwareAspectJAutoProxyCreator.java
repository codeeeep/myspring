package com.myspring;

import lombok.Setter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author codeep
 * @date 2024/3/16
 * @description:
 */
@Setter
public class AnnotationAwareAspectJAutoProxyCreator implements BeanPostProcessor {

    /**
     * 用于存储通过 @Before 注解解析出的切入点及通知
     */
    private ConcurrentHashMap<String, List<MethodWithClass>> beforeMethodMap = new ConcurrentHashMap<>();

    /**
     * 用于存储通过 @After 注解解析出的切入点及通知
     */
    private ConcurrentHashMap<String, List<MethodWithClass>> afterMethodMap = new ConcurrentHashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 想要完成 AOP 操作只需通过此方法对 bean 进行加工即可
     * @param bean  可能需要执行 AOP 操作的 Bean
     * @param beanName  可能需要执行 AOP 操作的 Bean 的 BeanName
     * @return  原本的 bean（无需 AOP） 或加工后的 bean（需要 AOP）
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 存储该 bean 的前置 methodName
        LinkedList<String> beforeMethods = new LinkedList<>();
        // 存储该 bean 的后置 methodName
        LinkedList<String> afterMethods = new LinkedList<>();
        // 填充上面两个 list
        fillMethodList(beanName, beforeMethods, afterMethods);
        // 判断是否需要执行 AOP 操作
        if (!beforeMethods.isEmpty() || !afterMethods.isEmpty()) {
            // 使用 JDK 动态代理来为 bean 创建一个动态代理对象
            Object proxy = Proxy.newProxyInstance(bean.getClass().getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    // 判断当前方法是否存在于 beforeMethods 列表中
                    if (beforeMethods.contains(method.getName())) {
                        // 拼接 beanName 和 methodName
                        String theKey = new StringBuilder(beanName).append('.').append(method.getName()).toString();
                        // 判断 beforeMethodMap 中是否存在相符的 key
                        if (beforeMethodMap.containsKey(theKey)) {
                            // 获取对应 List 中的所有 MethodWithClass 对象
                            List<MethodWithClass> methodWithClassList = beforeMethodMap.get(theKey);
                            // 依次调用这些方法
                            for (MethodWithClass methodWithClass : methodWithClassList) {
                                Object instance = methodWithClass.getClazz().getDeclaredConstructor().newInstance();
                                methodWithClass.getMethod().invoke(instance);
                            }
                        }
                    }

                    // 执行原本的方法
                    Object result = method.invoke(bean, args);

                    // 判断当前方法是否存在于 afterMethods 列表中
                    if (afterMethods.contains(method.getName())) {
                        // 拼接 beanName 和 methodName
                        String theKey = new StringBuilder(beanName).append('.').append(method.getName()).toString();
                        // 判断 beforeMethodMap 中是否存在相符的 key
                        if (afterMethodMap.containsKey(theKey)) {
                            // 获取对应 List 中的所有 MethodWithClass 对象
                            List<MethodWithClass> methodWithClassList = afterMethodMap.get(theKey);
                            // 依次调用这些方法
                            for (MethodWithClass methodWithClass : methodWithClassList) {
                                Object instance = methodWithClass.getClazz().getDeclaredConstructor().newInstance();
                                methodWithClass.getMethod().invoke(instance);
                            }
                        }
                    }
                    // 返回原本的返回值
                    return result;
                }
            });
            // 直接返回代理对象来代替 bean 对象
            return proxy;
        }
        return bean;
    }

    private void fillMethodList(String beanName, LinkedList<String> beforeMethods, LinkedList<String> afterMethods) {
        // 判断 beforeMethodMap 中有无传入的 beanName
        for (String key : beforeMethodMap.keySet()) {
            // 把 Map 中的 x.x 格式拆分成 beanName 和 methodName, 注意 . 需要转义，否则会当作正则匹配所有
            String[] beanAndMethod = key.split("\\.");
            if (beanAndMethod[0].equals(beanName)) {
                // 相同就把 methodName 先存入 List 中等待后面使用
                beforeMethods.add(beanAndMethod[1]);
            }
        }
        // 判断 afterMethodMap 中有无传入的 beanName
        for (String key : afterMethodMap.keySet()) {
            // 把 Map 中的 x.x 格式拆分成 beanName 和 methodName
            String[] beanAndMethod = key.split("\\.");
            // 判断数组中的 beanName 是否与传入的 beanName 相同
            if (beanAndMethod[0].equals(beanName)) {
                // 相同就把 methodName 先存入 List 中等待后面使用
                afterMethods.add(beanAndMethod[1]);
            }
        }
    }
}
