package com.myspring;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
     * 二级缓存单例池，存放还没进行属性赋值的半成品单例 Bean
     */
    private ConcurrentHashMap<String, Object> earlySingletonObjects = new ConcurrentHashMap();

    /**
     * 三级缓存
     */
    private ConcurrentHashMap<String, ObjectFactory> singletonFactories = new ConcurrentHashMap<>();

    /**
     * BeanDefinition 对象存储空间
     */
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    /**
     * 用于存储 BeanPostProcessor 实现类
     */
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    /**
     * 用于存储通过 @Before 注解解析出的切入点及通知
     * key: execute 表达式
     * value: 同一个 execute 表达式的所有增强方法
     */
    private ConcurrentHashMap<String, List<MethodWithClass>> beforeMethodMap = new ConcurrentHashMap<>();

    /**
     * 用于存储通过 @After 注解解析出的切入点及通知
     */
    private ConcurrentHashMap<String, List<MethodWithClass>> afterMethodMap = new ConcurrentHashMap<>();

    /**
     * 正在创建中的 Bean 的名称
     */
    private LinkedList<String> creatingBeanNameList = new LinkedList<>();

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
        // 判断配置类是否开启 AOP
        checkAop();
        // 在扫描完成后创建所有的单例 Bean 并放入单例池中
        createAllSingletons();
    }

    /**
     * 判断传入的参数 bean 是否需要 AOP，如果需要就返回代理对象，否则返回原始对象
     * @param beanName  传入 bean 的 BeanName
     * @param bean  传入待判断的 bean 对象
     * @return  根据传入参数 bean 对象来返回原始对象或代理对象
     */
    private Object getEarlyBeanReference(String beanName, Object bean) {
        // 创建一个额外的引用用于最终的返回
        Object exposedObject = bean;
        // 遍历 beanPostProcessorList
        for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
            // 判断其是否为 AnnotationAwareAspectJAutoProxyCreator 的实例
            if (beanPostProcessor instanceof AnnotationAwareAspectJAutoProxyCreator) {
                // 如果是，就调用后置处理方法，对 bean 进行加工
                exposedObject = beanPostProcessor.postProcessAfterInitialization(bean, beanName);
            }
        }
        return exposedObject;
    }

    /**
     * 判断配置类是否开启了 AOP，如果开启就向 Bean 容器中添加一个 AnnotationAwareAspectJAutoProxyCreator
     */
    private void checkAop() {
        // 判断类上是否有 @EnableAspectJAutoProxy 注解
        if (configClass.isAnnotationPresent(EnableAspectAutoProxy.class)) {
            // 如果存在此注解，便先获取 AnnotationAwareAspectJAutoProxyCreator 的 Class 对象
            Class clazz = AnnotationAwareAspectJAutoProxyCreator.class;
            try {
                // 将 AnnotationAwareAspectJAutoProxyCreator 实例化
                AnnotationAwareAspectJAutoProxyCreator instance = (AnnotationAwareAspectJAutoProxyCreator) clazz.getDeclaredConstructor().newInstance();
                // 填充这个实例的两个属性
                instance.setAfterMethodMap(afterMethodMap);
                instance.setBeforeMethodMap(beforeMethodMap);
                // 把实例存入 beanPostProcessor 池子中
                beanPostProcessorList.add(instance);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 在扫描完成后创建所有的单例 Bean 并放入单例池中
     */
    private void createAllSingletons() {
        // 获取 beanDefinitionMap 中的所有键值对
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            // 拿到 beanName
            String beanName = entry.getKey();
            // 拿到 BeanDefinition 对象
            BeanDefinition beanDefinition = entry.getValue();
            // 判断是否为单例模式
            if ("singleton".equals(beanDefinition.getScope())) {
                // 是单例模式，就立即创建对象
                Object bean = getBean(beanName);
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
            // 获取 BeanDefinition 对象
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            // 存在意味着之前定义过，那么就需要判断它的作用域
            if ("singleton".equals(beanDefinition.getScope())) {
                // 单例模式直接取出即可
                Object bean = singletonObjects.get(beanName);
                // 如果取不到，且这个 Bean 正在创建，就尝试从二级缓存中取出
                if (bean == null && creatingBeanNameList.contains(beanName)) {
                    bean = earlySingletonObjects.get(beanName);
                }
                // 如果仍然取不到，且这个 Bean 正在创建，就从三级缓存中去取，并放入二级缓存
                if (bean == null && creatingBeanNameList.contains(beanName)) {
                    // 从三级缓存中取出，调用 getObject() 方法来执行 getEarlyBeanReference() 方法
                    bean = singletonFactories.get(beanName).getObject();
                    // 放入二级缓存
                    earlySingletonObjects.put(beanName, bean);
                    // 从三级缓存中删除
                    singletonFactories.remove(beanName);
                }
                // 否则直接调用 createBean() 方法创建对象
                if (bean == null) {
                    bean = createBean(beanName, beanDefinition);
                }
                return bean;
            } else {
                // 不在单例池中存在，意味着是原型模式，需要创建对象
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
                                // 把实现了 BeanPostProcessor 接口的类实例添加到 list 中
                                addToBeanPostProcessorList(clazz);
                                // 对添加了 @Aspect 注解的类执行额外操作
                                getPointcutFromAspect(clazz);
                                // 创建当前类的 BeanDefinition 对象并添加到 map 中
                                putInBeanDefinitionMap(clazz);
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 判断传入的类是否是切面类，如果是切面类就进行解析
     * @param clazz
     */
    private void getPointcutFromAspect(Class<?> clazz) {
        // 判断是否添加了 @Aspect 注解
        if (clazz.isAnnotationPresent(Aspect.class)) {
            // 遍历这个类的全部方法
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                // 判断是否添加了 @Before 注解
                if (declaredMethod.isAnnotationPresent(Before.class)) {
                    // 获取 @Before 注解
                    Before beforeAnnotation = declaredMethod.getDeclaredAnnotation(Before.class);
                    // 获取注解中的参数
                    String pointcut = beforeAnnotation.value();
                    // 将解析出的参数和方法本身填入 map
                    List<MethodWithClass> methodWithClassList = null;
                    if (beforeMethodMap.get(pointcut) == null) {
                        methodWithClassList = new LinkedList<>();
                    }
                    else {
                        methodWithClassList = beforeMethodMap.get(pointcut);
                    }
                    MethodWithClass methodWithClass = new MethodWithClass(clazz, declaredMethod);
                    methodWithClassList.add(methodWithClass);
                    beforeMethodMap.put(pointcut, methodWithClassList);
                }
                // 判断是否添加了 @After 注解
                if (declaredMethod.isAnnotationPresent(After.class)) {
                    // 获取 @After 注解
                    After afterAnnotation = declaredMethod.getDeclaredAnnotation(After.class);
                    // 获取注解中的参数
                    String pointcut = afterAnnotation.value();
                    // 将解析出的参数和方法本身填入 map
                    List<MethodWithClass> methodWithClassList = null;
                    if (afterMethodMap.get(pointcut) == null) {
                        methodWithClassList = new LinkedList<>();
                    }
                    else {
                        methodWithClassList = afterMethodMap.get(pointcut);
                    }
                    MethodWithClass methodWithClass = new MethodWithClass(clazz, declaredMethod);
                    methodWithClassList.add(methodWithClass);
                    afterMethodMap.put(pointcut, methodWithClassList);
                }
            }
        }
    }

    /**
     * 把实现了 BeanPostProcessor 接口的对象实例放进池子中
     * @param clazz 实现了 BeanPostProcessor 接口的类对象
     *
     */
    private void addToBeanPostProcessorList(Class<?> clazz) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        // 判断这个类是否实现了 BeanPostProcessor 接口
        if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
            // 如果实现了 BeanPostProcessor 接口，就直接实例化
            BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
            // 把实例存入池子中
            beanPostProcessorList.add(instance);
        }
    }

    /**
     * 根据传入的 CLass 对象创建 BeanDefinition 对象并存入 Map 中
     * @param clazz 需要创建实例的类对象
     */
    private void putInBeanDefinitionMap(Class<?> clazz) {
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
            // 把 Bean 对象的名称放入 creatingBeanNameList 中
            creatingBeanNameList.add(beanName);
            // 把 Bean 对象及其名称放入三级缓存
            Object finalBean = bean;
            singletonFactories.put(beanName, () -> getEarlyBeanReference(beanName, finalBean));
            // 填充 Bean 对象的属性
            populateBean(clazz, bean);
            // 把 Bean 对象从三级缓存中删除
            singletonFactories.remove(beanName);
            // 判断当前 Bean 是否实现了 BeanNameAware 接口
            if (bean instanceof BeanNameAware) {
                // 如果实现了 BeanNameAware 接口，就可以直接强转，然后调用 setBeanName() 方法
                ((BeanNameAware) bean).setName(beanName);
            }
            // 调用 beanPostProcessorList 中所有实现类的 postProcessBeforeInitialization() 方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                bean = beanPostProcessor.postProcessBeforeInitialization(bean, beanName);
            }
            // 判断当前 Bean 是否实现了 InitializeBean 接口
            if (bean instanceof InitializeBean) {
                ((InitializeBean) bean).afterPropertiesSet();
            }
            // 调用 beanPostProcessorList 中所有实现类的 postProcessAfterInitialization() 方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                bean = beanPostProcessor.postProcessAfterInitialization(bean, beanName);
            }
            // 判断二级缓存中是否存在 Bean 对象
            if (earlySingletonObjects.containsKey(beanName)) {
                // 如果存在意味着它提前 AOP 了，需要取出
                bean = earlySingletonObjects.get(beanName);
            }
            // 把单例的 Bean 对象放入单例池
            if ("singleton".equals(beanDefinition.getScope())) {
                singletonObjects.put(beanName, bean);
            }
            // 从 creatingBeanNameList 中删除 BeanName
            creatingBeanNameList.remove(beanName);
            return bean;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 填充 Bean 对象的属性
     * @param clazz Bean 对象的 Class 对象
     * @param bean  待填充的 bean 对象
     * @throws IllegalAccessException
     */
    private void populateBean(Class clazz, Object bean) throws IllegalAccessException {
        // 遍历该类的全部属性准备进行属性填充
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
    }

}
