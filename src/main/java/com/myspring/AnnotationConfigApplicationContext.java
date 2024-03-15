package com.myspring;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author codeep
 * @date 2024/3/15 10:47
 * @description:
 */
public class AnnotationConfigApplicationContext {

    /**
     * 用户传来的配置类
     */
    private Class configClass;

    /**
     * 单例池
     */
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap();

    /**
     * BeanDefinition 对象存储空间
     */
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    /**
     * 初始化 IoC 容器
     *
     * @param configClass 配置类
     */
    public AnnotationConfigApplicationContext(Class configClass) {
        // 接收传入的配置类
        this.configClass = configClass;
        // 扫描配置类中指定的路径
        scan(configClass);
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if ("singleton".equals(beanDefinition.getScope())) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }
    }

    /**
     * 获取 Bean 对象
     *
     * @param beanName Bean 对象的名称
     * @return Bean 对象
     */
    public Object getBean(String beanName) {
        // 判断传入的参数是否在 beanDefinitionMap 中定义过
        if (beanDefinitionMap.containsKey(beanName)) {
            // 存在意味着之前定义过，那么就需要判断它的作用域
            if (singletonObjects.containsKey(beanName)) {
                // 在单例池中存在，意味着是单例模式，直接取出即可
                Object bean = singletonObjects.get(beanName);
                return bean;
            } else {
                // 不在单例池中存在，意味着是原型模式，需要创建对象
                BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
                Object bean = createBean(beanName, beanDefinition);
                return bean;
            }
        } else {
            // 不存在意味着没有定义，则抛出异常（推荐自定义异常，这里用空指针代替）
            throw new NullPointerException("池中无对象");
        }
    }

    /**
     * 扫描包下的注解
     *
     * @param configClass 配置类
     */
    private void scan(Class configClass) {
        // 判断类上是否有 @ComponentScan 注解
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            // 获取类上的 @ComponentScan 注解
            ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
            // 得到扫描路径
            String scanPath = componentScanAnnotation.value();
            // 获取应用类加载器
            ClassLoader classLoader = AnnotationConfigApplicationContext.class.getClassLoader();
            // 通过应用类加载器来获取资源路径
            URL resource = classLoader.getResource(scanPath.replace('.', '/'));
            // 把资源路径转化成文件
            File file = new File(resource.getFile());
            // 判断此文件是否是一个目录文件
            if (file.isDirectory()) {
                // 获取此目录下的所有文件
                File[] files = file.listFiles();
                // 遍历所有的文件
                for (File f : files) {
                    // 将文件名转化为全限定名
                    String fileName = f.getName();
                    if (fileName.endsWith(".class")) {
                        // 将文件名转化成类的全限定名
                        String className = scanPath.concat(".").concat(fileName.substring(0, fileName.lastIndexOf('.')));
                        try {
                            // 通过类加载器加载类文件来获取 Class 对象
                            Class<?> clazz = classLoader.loadClass(className);
                            // 通过每个类的 Class 对象判断该类是否添加了 @Component 注解
                            if (clazz.isAnnotationPresent(Component.class)) {
                                // 获取类的 @Component 注解
                                Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);
                                // 获取注解的参数
                                String beanName = componentAnnotation.value();
                                // 创建 BeanDefinition 对象
                                BeanDefinition beanDefinition = new BeanDefinition();
                                // 为 BeanDefinition 注入 clazz 属性
                                beanDefinition.setClazz(clazz);
                                // 判断类上是否添加了 @Scope 注解
                                if (clazz.isAnnotationPresent(Scope.class)) {
                                    // 获取类上的 @Scope 注解
                                    Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                                    // 获取此注解的参数，即 Bean 的作用域
                                    String scope = scopeAnnotation.value();
                                    // 把这个作用域赋值给 beanDefinition 对象
                                    beanDefinition.setScope(scope);
                                } else {
                                    // 没有配置 @Scope 注解的默认是单例模式
                                    beanDefinition.setScope("singleton");
                                }
                                // 把 BeanDefinition 对象放入 Map
                                beanDefinitionMap.put(beanName, beanDefinition);
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 根据 Bean 的定义创建 Bean 对象
     *
     * @param beanName Bean 的名称
     * @param beanDefinition Bean 的定义
     * @return Bean 对象
     */
    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        // 获取要创建的 Bean 对象的 Class 对象
        Class clazz = beanDefinition.getClazz();
        try {
            // 通过反射创建对象
            Object bean = clazz.getDeclaredConstructor().newInstance();
            // 遍历该类的全部属性
            for (Field declaredField : clazz.getDeclaredFields()) {
                // 判断属性上是否存在 @Autowired 注解
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    // 把属性名作为参数传递到 getBean() 方法中来获取对象
                    Object fieldBean = getBean(declaredField.getName());
                    if (fieldBean == null) {
                        Autowired autowiredAnnotation = declaredField.getDeclaredAnnotation(Autowired.class);
                        if (autowiredAnnotation.required()) {
                            throw new NullPointerException("参数注入错误");
                        }
                    }
                    // 破坏属性的私有
                    declaredField.setAccessible(true);
                    // 将 fieldBean 注入 bean 的属性值
                    declaredField.set(bean, fieldBean);
                }
            }
            // 判断当前 Bean 是否实现了 BeanNameAware 接口
            if (bean instanceof BeanNameAware) {
                // 如果实现了 BeanNameAware 接口，就可以直接强转，然后调用 setBeanName() 方法
                ((BeanNameAware) bean).setName(beanName);
            }
            return bean;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

}
