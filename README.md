# 手写 Spring 框架 开发记录

> 注：代码块中所有的 // ... ... 注释均表示与当前功能无关的代码，为了精简篇幅故而被省略

## 1. 启动时扫描

### 1.1 准备工作

1. 通过模板创建一个**普通的 Maven 项目**，并将项目结构按如下方式补充完整：

   ```
   - src
     - main
       - java
         - com
           - myspring（这个目录用于存放我们手写的Spring框架代码）
           - cy（这个目录用于模仿我们使用框架时创建的目录，名称随意）
       - resources
     - test
       - java
       - resources
   ```

2. 在 java/com/myspring 目录下创建一个 **AnnotationConfigApplicationContext 类**，并预先创建好以后会使用到的 API。

   AnnotationConfigApplicationContext.java：

   ```java
   package com.myspring;
   
   /**
    * 以配置类为参数的 Spring 容器
    */
   public class AnnotationConfigApplicationContext {
       /**
        * 配置文件的 Class 对象
        */
       private Class configClass;
   
       /**
        * 无参构造
        */
       public AnnotationConfigApplicationContext() {
       }
   
       /**
        * 传入 Class 对象的有参构造
        * @param configClass 配置类的 Class 对象
        */
       public AnnotationConfigApplicationContext(Class configClass) {
           this.configClass = configClass;
       }
   
       /**
        * 传入字符串参数的 getBean 方法
        * @param beanName 要获取对象的 JavaBean 的名称
        * @return 要取出的 JavaBean 对象
        */
       public Object getBean(String beanName) {
           return null;
       }
   }
   ```

3. 在 java/com/cy 目录下创建一个测试类，用于模仿我们在使用 Spring 框架时要写的代码。

   Test.java：

   ```java
   package com.cy;
   
   import com.myspring.AnnotationConfigApplicationContext;
   
   public class Test {
       public static void main(String[] args) {
           AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
           Object bean = context.getBean("");
       }
   }
   ```

   可见，我们需要再写一个配置类来作为参数，传入 AnnotationConfigApplicationContext 的有参构造中。

4. 在 java/com/cy 目录下创建一个配置类，准备将其作为参数传递给我们写的有参构造方法。

   AppConfig.java：

   ```java
   package com.cy;
   
   public class AppConfig {
   }
   ```

   在原版的 Spring 框架中，当我们编写好一个配置类后，通常会为其添加一个扫描注解，来扫描某个指定的包下使用的 Spring 注解。现在，我们就来创建这样一个注解。

5. 在 java/com/myspring 目录下创建一个 `@ComponentScan` 注解。

   ComponentScan.java：

   ```java
   package com.myspring;
   
   import java.lang.annotation.ElementType;
   import java.lang.annotation.Retention;
   import java.lang.annotation.RetentionPolicy;
   import java.lang.annotation.Target;
   
   @Target(ElementType.TYPE)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface ComponentScan {
       // 扫描路径
       String value();
   }
   ```

   用户通过 `@ComponentScan` 注解配置了扫描路径后，该路径下的注解就会被 Spring 所扫描到。那么，这些能被扫描到的注解具体都是哪些呢？没错，就是 `@Component` 以及它的变种注解。

6. 在 java/com/myspring 目录下创建一个 `@Component` 注解。

   Component.java：

   ```java
   package com.myspring;
   
   import java.lang.annotation.ElementType;
   import java.lang.annotation.Retention;
   import java.lang.annotation.RetentionPolicy;
   import java.lang.annotation.Target;
   
   @Target(ElementType.TYPE)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Component {
       // beanName
       String value() default "";
   }
   ```

7. 除此之外，我们还需要在 java/com/myspring 目录下定义一个 `@Scope` 注解，它能声明 **Bean 的作用域**。

   Scope.java：

   ```java
   package com.myspring;
   
   import java.lang.annotation.ElementType;
   import java.lang.annotation.Retention;
   import java.lang.annotation.RetentionPolicy;
   import java.lang.annotation.Target;
   
   @Target(ElementType.TYPE)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Scope {
       // beanName
       String value();
   }
   ```

8. 定义好注解之后，我们就要去配置类中使用了。但是在此之前，我们需要在 java/com/cy 目录下新建一个 service 目录，并在其中创建一个 UserService 接口及其实现类 UserServiceImpl，然后为实现类添加 `@Component("userService")` 注解。

   UserService.java：

   ```java
   package com.cy.service;
   
   public interface UserService {
   }
   ```

   UserServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Component;
   
   @Component("userService")
   @Scope("prototype")
   public class UserServiceImpl implements UserService {
   }
   ```

9. 之后，我们还需要在配置类中通过注解来配置扫描路径。

   AppConfig.java：

   ```java
   package com.cy;
   
   import com.myspring.ComponentScan;
   
   @ComponentScan("com.cy.service")
   public class AppConfig {
   }
   ```

10. 接下来，我们就在测试类中将此配置类的 Class 对象作为参数传递至有参构造中了。

    Test.java：

    ```java
    package com.cy;
    
    import com.myspring.AnnotationConfigApplicationContext;
    
    public class Test {
        public static void main(String[] args) {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
            Object userService = context.getBean("userService");
        }
    }
    ```

### 1.2 功能实现

我们希望在 AnnotationConfigApplicationContext 的有参构造方法中传入参数后，该类可以去**解析传入的配置类参数**，并**执行相关的处理**。

1. 判断配置类上是否存在 `@ComponentScan` 注解，如果存在，我们就进行下一步的处理。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 传入 Class 对象的有参构造
    * @param configClass 配置类的 Class 对象
    */
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
       // 判断类上是否有 @ComponentScan 注解
       if (configClass.isAnnotationPresent(ComponentScan.class)) {
       }
   }
   ```

2. 获取配置类上方的 `@ComponentScan` 注解，并得到其中的参数。这个参数就是我们接下来要去扫描的路径。

   AnnotationConfigApplicationContext.java：

   ```java
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
       // 判断类上是否有 @ComponentScan 注解
       if (configClass.isAnnotationPresent(ComponentScan.class)) {
           // 获取类上的 @ComponentScan 注解
           ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
           // 得到扫描路径
           String scanPath = componentScanAnnotation.value();
       }
   }
   ```

3. 得到了扫描路径，我们就可以具体地去执行扫描了。首先，我们需要获取**应用类加载器**。

   AnnotationConfigApplicationContext.java：

   ```java
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
       // 判断类上是否有 @ComponentScan 注解
       if (configClass.isAnnotationPresent(ComponentScan.class)) {
           // 获取类上的 @ComponentScan 注解
           ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
           // 得到扫描路径
           String scanPath = componentScanAnnotation.value();
           // 获取应用类加载器
           ClassLoader classLoader = AnnotationConfigApplicationContext.class.getClassLoader();
       }
   }
   ```

4. 接着，我们需要**借助应用类加载器来获取资源路径**。需要注意的是，我们接收到的参数是类似 `com.cy.service` 这样的包名，但我们需要的却是类似于 `com/cy/service` 这样的相对路径，因此需要进行一次转化。

   AnnotationConfigApplicationContext.java：

   ```java
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
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
       }
   }
   ```

5. 拿到了资源路径，我们就可以**将资源路径转化为一个文件**。

   AnnotationConfigApplicationContext.java：

   ```java
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
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
       }
   }
   ```

6. 只要转化后的文件是一个**目录文件**，我们就可以**遍历此目录下的所有文件**。

   AnnotationConfigApplicationContext.java：

   ```java
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
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
               }
           }
       }
   }
   ```

7. 得到了这些文件，我们就可以通过**让类加载器去加载这些文件**的方式来**获取 Class 对象**。

   AnnotationConfigApplicationContext.java：

   ```java
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
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
                   // 通过类加载器加载类文件来获取 Class 对象
                   Class<?> clazz = classLoader.loadClass("");
               }
           }
       }
   }
   ```

8. 但是在此之前，由于类加载器中需要传入的参数是**类文件的全限定名**，而我们只拿到了**全部的文件名**，因此还需要进行一次**格式过滤**及**文件名的转化**。

   AnnotationConfigApplicationContext.java：

   ```java
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
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
                   // 判断文件是不是类文件
                   if (fileName.endsWith(".class")) {
                       // 将文件名转化成类的全限定名
                       String className = scanPath.concat(".").concat(fileName.substring(0, fileName.lastIndexOf('.')));
                       try {
                           // 通过类加载器加载类文件来获取 Class 对象
                           Class<?> clazz = classLoader.loadClass(className);
                       } catch (ClassNotFoundException e) {
                           e.printStackTrace();
                       }
                   }
               }
           }
       }
   }
   ```

9. 得到**每个类文件的 Class 对象**之后，我们就可以判断**这个类上是否添加了 `@Component` 注解**。如果添加了此注解，便意味着这个类是一个交由 Spring 去托管的 bean。这样一来，我们就可以获取注解中的参数，这个参数便是用户定义的 beanName。

   AnnotationConfigApplicationContext.java：

   ```java
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
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
                   // 判断文件是不是类文件
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
                           }
                       } catch (ClassNotFoundException e) {
                           e.printStackTrace();
                       }
                   }
               }
           }
       }
   }
   ```

10. Spring 中的 Bean 的作用域主要可分为**单例 Bean**和**原型 Bean**两种。它们最显而易见的区别在于，每次调用 `getBean()` 方法获得的对象是否为同一个对象。单例 Bean 每次获取的对象都是同一个，而原型 Bean 获取的则不是同一个。不仅如此，它们创建的时机也不同。单例 Bean 早在扫描时就已经被创建，且被放入一个**单例池**中。我们可以通过一个私有属性 ConcurrentHashMap 来实现单例池：

    AnnotationConfigApplicationContext.java：

    ```java
    /**
    * 单例池
    */
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();
    ```

11. 如果是单例 Bean 就创建对象并存入单例池中，如果是原型 Bean 就先不创建。当然，这并不意味着对于原型 Bean 我们就什么都不用做了。我们希望后续调用 `getBean()` 时不需要反复地去解析传入的字符串，那么无论是单例的 Bean 还是原型的 Bean，都需要经过一定的处理。在 Spring 中，此过程名为 **BeanDefinition**，Spring 会去创建其对应的 BeanDefinition 对象。因此，我们需要先在 java/com/myspring 目录下创建一个 BeanDefinition 类：

    BeanDefinition.java：

    ```java
    package com.myspring;
    
    /**
     * Bean 定义类
     */
    public class BeanDefinition {
        /**
         * Bean 的类型
         */
        private Class clazz;
        /**
         * Bean 的作用域
         */
        private String Scope;
    
        public BeanDefinition() {
        }
    
        public BeanDefinition(Class clazz, String scope) {
            this.clazz = clazz;
            Scope = scope;
        }
    
        public Class getClazz() {
            return clazz;
        }
    
        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }
    
        public String getScope() {
            return Scope;
        }
    
        public void setScope(String scope) {
            Scope = scope;
        }
    }
    ```

12. 那么，我们就可以在确认了某类添加了 `@Component` 注解后创建一个 **BeanDefinition 对象**，并为其**填充 clazz 属性**。

    AnnotationConfigApplicationContext.java（代码量太长，从此处开始只展示判断是否添加 `@Component` 注解之后的代码）：

    ```java
    // 获取类的 @Component 注解
    Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);
    // 获取注解的参数
    String beanName = componentAnnotation.value();
    // 创建 BeanDefinition 对象
    BeanDefinition beanDefinition = new BeanDefinition();
    // 为 BeanDefinition 注入 clazz 属性
    beanDefinition.setClazz(clazz);
    ```

13. 接下来，我们就需要去**判断这个 Bean 对象究竟是单例 Bean 还是原型 Bean**了。首先，我们需要判断此类是否添加了 `@Scope` 注解。

    AnnotationConfigApplicationContext.java：

    ```java
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
    }
    else{
    }
    ```

14. 如果添加了 `@Scope` 注解，我们就要获取这个注解及其参数。

    AnnotationConfigApplicationContext.java：

    ```java
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
    }
    else{
    }
    ```

15. 拿到了参数，也就是 Bean 的作用域，我们就需要**把作用域填入 beanDefinition 对象**。

    AnnotationConfigApplicationContext.java：

    ```java
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
    }
    else {
        // 没有配置 @Scope 注解的默认是单例模式
        beanDefinition.setScope("singleton");
    }
    ```

16. BeanDefinition 对象的两个属性都赋好值后，需要一个空间来储存它。因此，我们像单例池那样，**通过一个 ConcurrentHashMap 来存储创建的所有 BeanDefinition 对象**。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * BeanDefinition 对象存储空间
     */
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    ```

17. 然后我们就可以把 BeanDefinition 对象放入 map 中了。

    AnnotationConfigApplicationContext.java：

    ```java
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
        }
        else {
            // 没有配置 @Scope 注解的默认是单例模式
            beanDefinition.setScope("singleton");
        }
        // 把 BeanDefinition 对象放入 Map
        beanDefinitionMap.put(beanName, beanDefinition);
    }
    ```

18. 由于代码量过于庞大，我们可以将之前写的有参构造中所有扫描的代码**抽象成一个私有方法**，取名为 `scan()`。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 对配置类中指定的路径进行扫描
     * @param configClass 配置类的 Class 对象
     */
    private void scan(Class configClass) {
        // 判断类上是否有 @Component 注解
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
                    // 判断文件是不是类文件
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
                                }
                                else {
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
    ```

19. 这样一来，有参构造中就十分精简了。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 传入 Class 对象的有参构造
     * @param configClass 配置类的 Class 对象
     */
    public AnnotationConfigApplicationContext(Class configClass) {
        // 接收传入的配置类
        this.configClass = configClass;
        // 扫描配置类中指定的路径
        scan(configClass);
    }
    ```

    当然，有参构造要做的事情并没有就此结束。只不过，做完这些之后，`getBean()` 方法的准备工作就完成了。因此，我们就可以先去编写一些 `getBean()` 方法的代码。

20. 首先，我们需要**判断传入的参数是否可以在 beanDefinitionMap 中找到**，这意味着我们是否曾定义过它。如果**没定义过**，就直接**抛出异常**。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 传入字符串参数的 getBean 方法
     * @param beanName 要获取对象的 JavaBean 的名称
     * @return 要取出的 JavaBean 对象
     */
    public Object getBean(String beanName) {
        // 判断传入的参数是否在 beanDefinitionMap 中定义过
        if (beanDefinitionMap.containsKey(beanName)) {
            // 存在意味着之前定义过，可以进行后续操作
        }
        else {
            // 不存在意味着没有定义，则抛出异常（推荐自定义异常，这里用空指针代替）
            throw new NullPointerException();
        }
        return null;
    }
    ```

21. 假如这个 Bean 是已经定义过的，那么我们就需要**判断它的作用域是单例模式还是原型模式**，判断的方式是**查看单例池中是否存在这个 Bean**。

    AnnotationConfigApplicationContext.java：

    ```java
    public Object getBean(String beanName) {
        // 判断传入的参数是否在 beanDefinitionMap 中定义过
        if (beanDefinitionMap.containsKey(beanName)) {
            // 存在意味着之前定义过，那么就需要判断它的作用域
            if (singletonObjects.containsKey(beanName)) {
                // 在单例池中存在，意味着是单例模式，直接取出即可
            }
            else {
                // 不在单例池中存在，意味着是原型模式，需要创建对象
            }
        }
        else {
            // 不存在意味着没有定义，则抛出异常（推荐自定义异常，这里用空指针代替）
            throw new NullPointerException();
        }
        return null;
    }
    ```

22. **如果是单例模式**，**就从单例池中直接取出即可**，并将取出的 Bean 对象直接返回。

    AnnotationConfigApplicationContext.java：

    ```java
    public Object getBean(String beanName) {
        // 判断传入的参数是否在 beanDefinitionMap 中定义过
        if (beanDefinitionMap.containsKey(beanName)) {
            // 存在意味着之前定义过，那么就需要判断它的作用域
            if (singletonObjects.containsKey(beanName)) {
                // 在单例池中存在，意味着是单例模式，直接取出即可
                Object bean = singletonObjects.get(beanName);
                return bean;
            }
            else {
                // 不在单例池中存在，意味着是原型模式，需要创建对象
            }
        }
        else {
            // 不存在意味着没有定义，则抛出异常（推荐自定义异常，这里用空指针代替）
            throw new NullPointerException();
        }
        return null;
    }
    ```

    **如果是原型模式**，**则需要创建对象**。但是创建对象的过程或许没那么简单，因此无论是前面单例模式的创建，还是这里原型模式的创建，我们都先在原处留空。等我们把创建 Bean 对象的过程抽象称为一个方法之后，再回过头来填补空缺。

23. 创建私有方法 `createBean(BeanDefinition beanDefinition)`，用于创建 Bean 对象。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 创建 Bean 对象
     * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
     * @return 创建出来的 Bean 对象
     */
    private Object createBean(BeanDefinition beanDefinition) {
        return null;
    }
    ```

24. 对于**单例模式**的 Bean 对象而言，我们需要**在扫描结束后就进行创建**，因此需要在有参构造中的 `scan()` 方法后进行创建。首先，我们需要获取 beanDefinitionMap 中存储的所有键值对。

    AnnotationConfigApplicationContext.java：

    ```java
    public AnnotationConfigApplicationContext(Class configClass) {
        // 接收传入的配置类
        this.configClass = configClass;
        // 扫描配置类中指定的路径
        scan(configClass);
        // 获取 beanDefinitionMap 中的所有键值对
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            
        }
    }
    ```

25. 拿到这些键值对，我们就可以取出每一组对应的 beanName 和 BeanDefinition 对象。

    AnnotationConfigApplicationContext.java：

    ```java
    public AnnotationConfigApplicationContext(Class configClass) {
        // 接收传入的配置类
        this.configClass = configClass;
        // 扫描配置类中指定的路径
        scan(configClass);
        // 获取 beanDefinitionMap 中的所有键值对
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            // 拿到 beanName
            String beanName = entry.getKey();
            // 拿到 BeanDefinition 对象
            BeanDefinition beanDefinition = entry.getValue();
        }
    }
    ```

26. 这样一来，我们就可以**根据 BeanDefinition 对象来判断其是否为单例模式**了。

    AnnotationConfigApplicationContext.java：

    ```java
    public AnnotationConfigApplicationContext(Class configClass) {
        // 接收传入的配置类
        this.configClass = configClass;
        // 扫描配置类中指定的路径
        scan(configClass);
        // 获取 beanDefinitionMap 中的所有键值对
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            // 拿到 beanName
            String beanName = entry.getKey();
            // 拿到 BeanDefinition 对象
            BeanDefinition beanDefinition = entry.getValue();
            // 判断是否为单例模式
            if ("singleton".equals(beanDefinition.getScope())) {
                // 是单例模式
            }
        }
    }
    ```

27. 对于是单例模式的类，我们需要当即调用 `createBean()` 方法进行创建其 Bean 对象，并将此 Bean 对象放入单例池中。

    AnnotationConfigApplicationContext.java：

    ```java
    public AnnotationConfigApplicationContext(Class configClass) {
        // 接收传入的配置类
        this.configClass = configClass;
        // 扫描配置类中指定的路径
        scan(configClass);
        // 获取 beanDefinitionMap 中的所有键值对
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            // 拿到 beanName
            String beanName = entry.getKey();
            // 拿到 BeanDefinition 对象
            BeanDefinition beanDefinition = entry.getValue();
            // 判断是否为单例模式
            if ("singleton".equals(beanDefinition.getScope())) {
                // 是单例模式，就立即创建对象
                Object bean = createBean(beanDefinition);
                // 把 Bean 对象放入单例池
                singletonObjects.put(beanName, bean);
            }
        }
    }
    ```

28. 对于原型模式，则只需在 `getBean()` 方法中调用 `createBean()` 方法创建一个 Bean 对象并返回即可。

    AnnotationConfigApplicationContext.java：

    ```java
    public Object getBean(String beanName) {
        // 判断传入的参数是否在 beanDefinitionMap 中定义过
        if (beanDefinitionMap.containsKey(beanName)) {
            // 存在意味着之前定义过，那么就需要判断它的作用域
            if (singletonObjects.containsKey(beanName)) {
                // 在单例池中存在，意味着是单例模式，直接取出即可
                Object bean = singletonObjects.get(beanName);
                return bean;
            }
            else {
                // 不在单例池中存在，意味着是原型模式，需要创建对象
                BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
                Object bean = createBean(beanDefinition);
                return bean;
            }
        }
        else {
            // 不存在意味着没有定义，则抛出异常（推荐自定义异常，这里用空指针代替）
            throw new NullPointerException();
        }
    }
    ```

29. 接下来，就是 `createBean()` 方法的具体实现了。首先，我们应该把传入的参数利用起来，拿到我们希望创建的对象的 Class 对象，为后面的操作做准备。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 创建 Bean 对象
     * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
     * @return 创建出来的 Bean 对象
     */
    private Object createBean(BeanDefinition beanDefinition) {
        // 获取要创建的 Bean 对象的 Class 对象
        Class clazz = beanDefinition.getClazz();
        return null;
    }
    ```

30. 拿到了 Class 对象，我们就可以通过反射来创建对象了。

    AnnotationConfigApplicationContext.java：

    ```java
    private Object createBean(BeanDefinition beanDefinition) {
        // 获取要创建的 Bean 对象的 Class 对象
        Class clazz = beanDefinition.getClazz();
        // 通过反射创建对象
        try {
            Object bean = clazz.getDeclaredConstructor().newInstance();
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
    ```

### 1.3 测试结果

至此，一个最基本的扫描逻辑就已经实现了。我们可以在 UserService 中添加一个方法，验证这些代码是否能够成功运行。

UserService.java：

```java
package com.cy.service;

public interface UserService {
    void queryAllUsers();
}
```

UserServiceImpl.java：

```java
package com.cy.service;

import com.myspring.Component;
import com.myspring.Scope;

@Component("userService")
@Scope("prototype")
public class UserServiceImpl implements UserService {
    @Override
    public void queryAllUsers() {
        System.out.println("调用了方法：查询所有的用户");
    }
}
```

最后，我们在测试类中通过 `getBean()` 方法创建 UserServiceImpl 对象，并调用其 `queryAllUsers()` 方法，查看运行结果。

Test.java：

```java
package com.cy;

import com.myspring.AnnotationConfigApplicationContext;
import com.cy.service.UserService;

public class Test {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService userService = (UserService) context.getBean("userService");
        userService.queryAllUsers();
    }
}
```

输出结果：

```
调用了方法：查询所有的用户
```

除此之外，我们还可以通过以下方式来验证 Bean 的作用域的代码是否有问题：

Test.java：

```java
public class Test {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService userService1 = (UserService) context.getBean("userService");
        UserService userService2 = (UserService) context.getBean("userService");
        UserService userService3 = (UserService) context.getBean("userService");
        System.out.println(userService1 == userService2 && userService2 == userService3);
    }
}
```

此时，UserServiceImpl 已经添加了 `@Scope("prototype")` 注解，那么创建出的这三个对象应该是不同的对象，而测试类的输出结果如下：

```
false
```

我们将 UserServiceImpl 的注解改为 `@Scope("singleton")` 再进行测试，可以看到输出结果发生了变化：

```
true
```

如果我们删去 `@Scope` 注解，UserServiceImpl 的 Bean 对象将默认作为单例模式被创建。此时的输出结果为：

```
true
```



## 2. 依赖注入

### 2.1 准备工作

接下来，我们就应该去关心 `createBean()` 方法中的具体实现了。但是在此之前，我们需要先完成一些准备工作。

1. 在 java/com/myspring 目录下创建 `@Autowired` 注解。

   Autowired.java：

   ```java
   package com.myspring;
   
   import java.lang.annotation.ElementType;
   import java.lang.annotation.Retention;
   import java.lang.annotation.RetentionPolicy;
   import java.lang.annotation.Target;
   
   @Target({ElementType.FIELD, ElementType.METHOD})
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Autowired {
       boolean required() default true;
   }
   ```

2. 在 java/com/cy/service 目录下创建 OrderService 接口和 OrderServiceImpl 实现类。

   OrderService.java：

   ```java
   package com.cy.service;
   
   public interface OrderService {
   }
   ```

   OrderServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Component;
   
   @Component("orderService")
   public class OrderServiceImpl implements OrderService{
   }
   ```

3. 在 UserServiceImpl 中添加一个私有属性，类型为 OrderService，并添加 `@Autowired` 注解。

   UserServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.Component;
   import com.myspring.Scope;
   
   @Component("userService")
   @Scope("prototype")
   public class UserServiceImpl implements UserService {
       @Autowired
       private OrderService orderService;
   
       @Override
       public void queryAllUsers() {
           System.out.println("调用了方法：查询所有的用户");
       }
   }
   ```

4. 在 UserService 接口中添加一个方法，并在其实现类的方法中打印 orderService。

   UserService.java：

   ```java
   package com.cy.service;
   
   public interface UserService {
       void queryAllUsers();
       void methodWithOrders();
   }
   ```

   UserServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.Component;
   import com.myspring.Scope;
   
   @Component("userService")
   @Scope("prototype")
   public class UserServiceImpl implements UserService {
       @Autowired
       private OrderService orderService;
   
       @Override
       public void queryAllUsers() {
           System.out.println("调用了方法：查询所有的用户");
       }
   
       @Override
       public void methodWithOrders() {
           System.out.println(orderService);
       }
   }
   ```

5. 我们在测试类中调用此方法，其输出结果此时应当为 null。

   Test.java：

   ```java
   package com.cy;
   
   import com.myspring.AnnotationConfigApplicationContext;
   import com.cy.service.UserService;
   
   public class Test {
       public static void main(String[] args) {
           // 测试有参构造
           AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
           // 测试 getBean 方法
           UserService userService = (UserService) context.getBean("userService");
           // 测试依赖注入
           userService.methodWithOrders();
       }
   }
   ```

   输出结果：

   ```
   null
   ```

### 2.2 功能实现

到目前为止，我们在 `createBean()` 方法中是**通过反射无参构造器**来创建对象的，这样创建出来的对象其**属性都是默认值**。我们希望在创建对象时，**能够读取到属性上的 `@Autowired` 注解**，以此来实现**依赖注入**的功能。

1. 首先，我们需要根据解析出的 Class 对象来**遍历该类的所有属性**。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 创建 Bean 对象
    * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
    * @return 创建出来的 Bean 对象
    */
   private Object createBean(BeanDefinition beanDefinition) {
       // 获取要创建的 Bean 对象的 Class 对象
       Class clazz = beanDefinition.getClazz();
       try {
           // 通过反射创建 Bean 对象
           Object bean = clazz.getDeclaredConstructor().newInstance();
           // 遍历该类的全部属性
           for (Field declaredField : clazz.getDeclaredFields()) {
               
           }
           // 把 Bean 对象返回
           return bean;
       }
       catch (InstantiationException e) {
           e.printStackTrace();
       }
       catch (IllegalAccessException e) {
           e.printStackTrace();
       }
       catch (InvocationTargetException e) {
           e.printStackTrace();
       }
       catch (NoSuchMethodException e) {
           e.printStackTrace();
       }
       return null;
   }
   ```

2. 接着，我们就可以去判断这些属性上**是否添加了 `@Autowired` 注解**。

   AnnotationConfigApplicationContext.java（由于代码量过大，从此处开始只展示 try 代码块中间的部分）：

   ```java
   // 通过反射创建 Bean 对象
   Object bean = clazz.getDeclaredConstructor().newInstance();
   // 遍历该类的全部属性
   for (Field declaredField : clazz.getDeclaredFields()) {
       // 判断属性上是否存在 @Autowired 注解
       if (declaredField.isAnnotationPresent(Autowired.class)) {
   
       }
   }
   // 把 Bean 对象返回
   return bean;
   ```

3. 如果存在此注解，我们就可以**把属性名作为参数传入到 `getBean()` 方法中**，以此来获取对应的 Bean 对象，准备注入（**即 byName 形式的注入**）。这样做的好处是，`getBean()` 中已经判断了 Bean 对象的作用域，无需我们再去考虑是否需要创建一个新对象来注入的问题。

   AnnotationConfigApplicationContext.java：

   ```java
   // 通过反射创建 Bean 对象
   Object bean = clazz.getDeclaredConstructor().newInstance();
   // 遍历该类的全部属性
   for (Field declaredField : clazz.getDeclaredFields()) {
       // 判断属性上是否存在 @Autowired 注解
       if (declaredField.isAnnotationPresent(Autowired.class)) {
           // 把属性名作为参数传递到 getBean() 方法中来获取对象
           Object fieldBean = getBean(declaredField.getName());
       }
   }
   // 把 Bean 对象返回
   return bean;
   ```

4. 需要注意的是，此处我们未必一定能获取到 fieldBean。**如果 fieldBean 为 null**，**且 `@Autowired` 注解的 required 属性为 true**，那么就需要**抛出异常**。

   AnnotationConfigApplicationContext.java：

   ```java
   // 通过反射创建 Bean 对象
   Object bean = clazz.getDeclaredConstructor().newInstance();
   // 遍历该类的全部属性
   for (Field declaredField : clazz.getDeclaredFields()) {
       // 判断属性上是否存在 @Autowired 注解
       if (declaredField.isAnnotationPresent(Autowired.class)) {
           // 把属性名作为参数传递到 getBean() 方法中来获取对象
           Object fieldBean = getBean(declaredField.getName());
           // 如果获取的对象为 null，且 @Autowired 的 required 属性为 true，就需要抛出一个异常
           if (fieldBean == null) {
               Autowired autowiredAnnotation = declaredField.getDeclaredAnnotation(Autowired.class);
               if (autowiredAnnotation.required()) {
                   throw new NullPointerException();
               }
           }
       }
   }
   // 把 Bean 对象返回
   return bean;
   ```

5. 拿到待注入的 Bean 对象之后，我们可以**通过反射破坏属性的私有**，再**将 Bean 对象注入属性值**。

   AnnotationConfigApplicationContext.java：

   ```java
   // 通过反射创建 Bean 对象
   Object bean = clazz.getDeclaredConstructor().newInstance();
   // 遍历该类的全部属性
   for (Field declaredField : clazz.getDeclaredFields()) {
       // 判断属性上是否存在 @Autowired 注解
       if (declaredField.isAnnotationPresent(Autowired.class)) {
           // 把属性名作为参数传递到 getBean() 方法中来获取对象
           Object fieldBean = getBean(declaredField.getName());
           // 如果获取的对象为 null，且 @Autowired 的 required 属性为 true，就需要抛出一个异常
           if (fieldBean == null) {
               Autowired autowiredAnnotation = declaredField.getDeclaredAnnotation(Autowired.class);
               if (autowiredAnnotation.required()) {
                   throw new NullPointerException();
               }
           }
           // 破坏属性的私有
           declaredField.setAccessible(true);
           // 将 fieldBean 注入 bean 的属性值
           declaredField.set(bean, fieldBean);
       }
   }
   // 把 Bean 对象返回
   return bean;
   ```


### 2.3 测试结果

写完以上代码，我们重新回到测试类，运行以下代码查看结果：

Test.java：

```java
package com.cy;

import com.myspring.AnnotationConfigApplicationContext;
import com.cy.service.UserService;

public class Test {
    public static void main(String[] args) {
        // 测试有参构造
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        // 测试 getBean 方法
        UserService userService = (UserService) context.getBean("userService");
        // 测试依赖注入
        userService.methodWithOrders();
    }
}
```

输出结果：

```
com.cy.service.OrderServiceImpl@4dc63996
```

可以看到，OrderServiceImpl 对象已经被作为属性注入到 UserServiceImpl 的私有属性中了。



## 3. Aware 回调

原版的 Spring 框架为我们提供了一个 BeanNameAware 接口，它能**让实现了这个接口的  Java Bean 接收到自己的名字**，这个过程叫做 **Aware 回调**。至于程序员在拿到这个名字之后要进行什么操作，就与 Spring 无关了。现在，我们就来模仿实现一下这个功能。

### 3.1 准备工作

1. 在 java/com/myspring 目录下创建一个 **BeanNameAware 接口**，其中包含一个 `setName()` 方法。

   BeanNameAware.java：

   ```java
   package com.myspring;
   
   public interface BeanNameAware {
       void setName(String name);
   }
   ```

2. 为 UserServiceImpl 添加一个新的私有属性，取名为 beanName。

   UserServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.Component;
   import com.myspring.Scope;
   
   @Component("userService")
   @Scope("prototype")
   public class UserServiceImpl implements UserService {
       @Autowired
       private OrderService orderService;
   
       private String beanName;
   
       @Override
       public void queryAllUsers() {
           System.out.println("调用了方法：查询所有的用户");
       }
   
       @Override
       public void methodWithOrders() {
           System.out.println(orderService);
       }
   }
   ```

3. 令 UserServiceImpl 实现此接口，并重写 `setName()` 方法。

   UserServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.BeanNameAware;
   import com.myspring.Component;
   import com.myspring.Scope;
   
   @Component("userService")
   @Scope("prototype")
   public class UserServiceImpl implements UserService, BeanNameAware {
       @Autowired
       private OrderService orderService;
   
       private String beanName;
   
       @Override
       public void setName(String name) {
           this.beanName = name;
       }
   
       @Override
       public void queryAllUsers() {
           System.out.println("调用了方法：查询所有的用户");
       }
   
       @Override
       public void methodWithOrders() {
           System.out.println(orderService);
       }
   }
   ```

4. 在 UserService 接口中添加一个方法，并在其实现类的方法中打印 beanName。

   UserService.java：

   ```java
   package com.cy.service;
   
   public interface UserService {
       void queryAllUsers();
       void methodWithOrders();
       void printBeanName();
   }
   ```

   UserServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.BeanNameAware;
   import com.myspring.Component;
   import com.myspring.Scope;
   
   @Component("userService")
   @Scope("prototype")
   public class UserServiceImpl implements UserService, BeanNameAware {
       @Autowired
       private OrderService orderService;
   
       private String beanName;
   
       @Override
       public void setName(String name) {
           this.beanName = name;
       }
   
       @Override
       public void printBeanName() {
           System.out.println(beanName);
       }
   
       @Override
       public void queryAllUsers() {
           System.out.println("调用了方法：查询所有的用户");
       }
   
       @Override
       public void methodWithOrders() {
           System.out.println(orderService);
       }
   }
   ```

5. 在测试类中调用此方法，此时的输出结果应当为 null。

   Test.java：

   ```java
   package com.cy;
   
   import com.myspring.AnnotationConfigApplicationContext;
   import com.cy.service.UserService;
   
   public class Test {
       public static void main(String[] args) {
           // 测试有参构造
           AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
           // 测试 getBean 方法
           UserService userService = (UserService) context.getBean("userService");
           // 测试 BeanNameAware
           userService.printBeanName();
       }
   }
   ```

### 3.2 功能实现

想要实现这个功能，同样应当在 `createBean()` 方法中进行操作。我们可以在依赖注入之后，返回 Bean 对象之前，完成将 Bean 名称作为参数传入 `setName()` 方法的操作。

1. 首先我们需要判断当前的 bean 是否实现了 BeanNameAware 接口。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 创建 Bean 对象
    * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
    * @return 创建出来的 Bean 对象
    */
   private Object createBean(BeanDefinition beanDefinition) {
       // 获取要创建的 Bean 对象的 Class 对象
       Class clazz = beanDefinition.getClazz();
       try {
           // 通过反射创建 Bean 对象
           Object bean = clazz.getDeclaredConstructor().newInstance();
           // 遍历该类的全部属性
           for (Field declaredField : clazz.getDeclaredFields()) {
               // 判断属性上是否存在 @Autowired 注解
               if (declaredField.isAnnotationPresent(Autowired.class)) {
                   // 把属性名作为参数传递到 getBean() 方法中来获取对象
                   Object fieldBean = getBean(declaredField.getName());
                   // 如果获取的对象为 null，且 @Autowired 的 required 属性为 true，就需要抛出一个异常
                   if (fieldBean == null) {
                       Autowired autowiredAnnotation = declaredField.getDeclaredAnnotation(Autowired.class);
                       if (autowiredAnnotation.required()) {
                           throw new NullPointerException();
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
               
           }
           // 把 Bean 对象返回
           return bean;
       }
       catch (InstantiationException e) {
           e.printStackTrace();
       }
       catch (IllegalAccessException e) {
           e.printStackTrace();
       }
       catch (InvocationTargetException e) {
           e.printStackTrace();
       }
       catch (NoSuchMethodException e) {
           e.printStackTrace();
       }
       return null;
   }
   ```

2. 如果实现了 BeanNameAware 接口，就可以直接强转，然后调用 `setName()` 方法。

   AnnotationConfigApplicationContext.java：

   ```java
   // 判断当前 Bean 是否实现了 BeanNameAware 接口
   if (bean instanceof BeanNameAware) {
       // 如果实现了 BeanNameAware 接口，就可以直接强转，然后调用 setBeanName() 方法
       ((BeanNameAware) bean).setName();
   }
   ```

3. 不难注意到，此时我们需要传递的是 beanName，可是当前方法中只有 BeanDefinition 对象，而仅凭 BeanDefinition 对象无法获取 BeanName。因此，我们需要修改该方法接收的参数。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 创建 Bean 对象
    * @param beanName 要创建的 Bean 的名称
    * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
    * @return 创建出来的 Bean 对象
    */
   private Object createBean(String beanName, BeanDefinition beanDefinition) {
       //... ...
   }
   ```

4. 这样一来，我们就可以直接在 `setName()` 方法中传入 beanName 作为参数了。

   AnnotationConfigApplicationContext.java：

   ```java
   // 判断当前 Bean 是否实现了 BeanNameAware 接口
   if (bean instanceof BeanNameAware) {
       // 如果实现了 BeanNameAware 接口，就可以直接强转，然后调用 setName() 方法
       ((BeanNameAware) bean).setName(beanName);
   }
   ```

5. 做出这样的修改后，前面调用 `createBean()` 方法的地方（有参构造和 `getBean()` 方法）也必须做出修改，将 beanName 作为参数传递进来。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 传入 Class 对象的有参构造
    * @param configClass 配置类的 Class 对象
    */
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
       // 扫描配置类中指定的路径
       scan(configClass);
       // 获取 beanDefinitionMap 中的所有键值对
       for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
           // 拿到 beanName
           String beanName = entry.getKey();
           // 拿到 BeanDefinition 对象
           BeanDefinition beanDefinition = entry.getValue();
           // 判断是否为单例模式
           if ("singleton".equals(beanDefinition.getScope())) {
               // 是单例模式，就立即创建对象
               Object bean = createBean(beanName, beanDefinition);
               // 把 Bean 对象放入单例池
               singletonObjects.put(beanName, bean);
           }
       }
   }
   ```

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 传入字符串参数的 getBean 方法
    * @param beanName 要获取对象的 JavaBean 的名称
    * @return 要取出的 JavaBean 对象
    */
   public Object getBean(String beanName) {
       // 判断传入的参数是否在 beanDefinitionMap 中定义过
       if (beanDefinitionMap.containsKey(beanName)) {
           // 存在意味着之前定义过，那么就需要判断它的作用域
           if (singletonObjects.containsKey(beanName)) {
               // 在单例池中存在，意味着是单例模式，直接取出即可
               Object bean = singletonObjects.get(beanName);
               return bean;
           }
           else {
               // 不在单例池中存在，意味着是原型模式，需要创建对象
               BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
               Object bean = createBean(beanName, beanDefinition);
               return bean;
           }
       }
       else {
           // 不存在意味着没有定义，则抛出异常（推荐自定义异常，这里用空指针代替）
           throw new NullPointerException();
       }
   }
   ```

### 3.3 测试结果

写完以上代码，我们重新回到测试类，运行以下代码查看结果：

Test.java：

```java
package com.cy;

import com.myspring.AnnotationConfigApplicationContext;
import com.cy.service.UserService;

public class Test {
    public static void main(String[] args) {
        // 测试有参构造
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        // 测试 getBean 方法
        UserService userService = (UserService) context.getBean("userService");
        // 测试 BeanNameAware
        userService.printBeanName();
    }
}
```

输出结果：

```
userService
```

如此可见，userService 已经取到了我们通过 `@Component` 注解为其设定的 BeanName。

Aware 接口的目的就是为了在创建 Bean 的时候有个标记来判断是否会给 Bean 设置 BeanName



## 4. Bean 的初始化

原版 Spring 框架还为我们提供了一个 InitializeBean 接口，实现了这个接口的 Bean 在被创建时就会被初始化，采用的方式便是调用该接口下的 `afterPropertiesSet()` 方法。至于程序员如何重写此方法，就与 Spring 无关了。现在我们就来模拟实现此功能。

### 4.1 准备工作

1. 在 java/com/myspring 目录下创建一个 InitializeBean 接口，其中包含一个 `afterPropertiesSet()` 方法。

   InitializeBean.java：

   ```java
   package com.myspring;
   
   public interface InitializeBean {
       void afterPropertiesSet() throws Exception;
   }
   ```

2. 令 UserServiceImpl 实现此接口，并重写此方法，令其输出一句话，其中包含了通过 Aware 回调获取到的 BeanName。

   UserServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.BeanNameAware;
   import com.myspring.Component;
   import com.myspring.InitializeBean;
   import com.myspring.Scope;
   
   @Component("userService")
   @Scope("prototype")
   public class UserServiceImpl implements UserService, BeanNameAware, InitializeBean {
       @Autowired
       private OrderService orderService;
   
       private String beanName;
   
       @Override
       public void setName(String name) {
           this.beanName = name;
       }
   
       @Override
       public void printBeanName() {
           System.out.println(beanName);
       }
   
       @Override
       public void afterPropertiesSet() throws Exception {
           System.out.println(beanName + "被初始化了！");
       }
   
       @Override
       public void queryAllUsers() {
           System.out.println("调用了方法：查询所有的用户");
       }
   
       @Override
       public void methodWithOrders() {
           System.out.println(orderService);
       }
   }
   ```

3. 在测试类中创建 Bean 后什么都不做，观察输出结果，此时应当什么都不输出。

   Test.java：

   ```java
   package com.cy;
   
   import com.myspring.AnnotationConfigApplicationContext;
   import com.cy.service.UserService;
   
   public class Test {
       public static void main(String[] args) {
           AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
           // 测试 Bean 初始化
           UserService userService = (UserService) context.getBean("userService");
       }
   }
   ```

### 4.2 功能实现

我们在实现了 Aware 回调之后来解决 Bean 初始化的问题，方法与 Aware 回调相似。

1. 首先判断 Bean 是否实现了 InitializeBean 接口。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 创建 Bean 对象
    * @param beanName 要创建的 Bean 的名称
    * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
    * @return 创建出来的 Bean 对象
    */
   private Object createBean(String beanName, BeanDefinition beanDefinition) {
       // 获取要创建的 Bean 对象的 Class 对象
       Class clazz = beanDefinition.getClazz();
       try {
           // 通过反射创建 Bean 对象
           Object bean = clazz.getDeclaredConstructor().newInstance();
           // 遍历该类的全部属性
           for (Field declaredField : clazz.getDeclaredFields()) {
               // 判断属性上是否存在 @Autowired 注解
               if (declaredField.isAnnotationPresent(Autowired.class)) {
                   // 把属性名作为参数传递到 getBean() 方法中来获取对象
                   Object fieldBean = getBean(declaredField.getName());
                   // 如果获取的对象为 null，且 @Autowired 的 required 属性为 true，就需要抛出一个异常
                   if (fieldBean == null) {
                       Autowired autowiredAnnotation = declaredField.getDeclaredAnnotation(Autowired.class);
                       if (autowiredAnnotation.required()) {
                           throw new NullPointerException();
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
               // 如果实现了 BeanNameAware 接口，就可以直接强转，然后调用 setName() 方法
               ((BeanNameAware) bean).setName(beanName);
           }
           // 判断当前 Bean 是否实现了 InitializeBean 接口
           if (bean instanceof InitializeBean) {
               
           }
           // 把 Bean 对象返回
           return bean;
       }
       catch (InstantiationException e) {
           e.printStackTrace();
       }
       catch (IllegalAccessException e) {
           e.printStackTrace();
       }
       catch (InvocationTargetException e) {
           e.printStackTrace();
       }
       catch (NoSuchMethodException e) {
           e.printStackTrace();
       }
       return null;
   }
   ```

2. 如果实现了 InitializeBean 接口，就可以直接强转，然后调用 `afterPropertiesSet()` 方法。

   AnnotationConfigApplicationContext.java：

   ```java
   // 判断当前 Bean 是否实现了 InitializeBean 接口
   if (bean instanceof InitializeBean) {
       // 如果实现了 InitializeBean 接口，就可以直接强转，然后调用 afterPropertiesSet() 方法
       ((InitializeBean) bean).afterPropertiesSet();
   }
   ```

### 4.3 测试结果

写完以上代码，我们重新回到测试类，运行以下代码查看结果：

Test.java：

```java
package com.cy;

import com.myspring.AnnotationConfigApplicationContext;
import com.cy.service.UserService;

public class Test {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        // 测试 Bean 初始化
        UserService userService = (UserService) context.getBean("userService");
    }
}
```

输出结果：

```
userService被初始化了！
```

这就说明，userService 成功执行了初始化方法。



## 5. 后置处理器

原版 Spring 为我们提供了一个非常方便的**后置处理器 BeanPostProcessor**，它的实质是一个**接口**。当一个 Java 类实现了这个接口之后，程序员就能通过重写其中的两个方法的方式实现在 Bean 的初始化之前和 Bean 的初始化之后执行一些自定义的操作。当然，这些操作是所有 Bean 初始化前后都会触发的。如果想要针对单独的 Bean 做处理，程序员可以自行在方法中根据 BeanName 去筛选。

### 5.1 准备工作

1. 在 java/com/myspring 目录下创建一个 BeanPostProcessor 接口。

   BeanPostProcessor.java：

   ```java
   package com.myspring;
   
   public interface BeanPostProcessor {
       Object postProcessBeforeInitialization(Object bean, String beanName);
       Object postProcessAfterInitialization(Object bean, String beanName);
   }
   ```

2. 在 java/com/cy/service 目录下创建一个 TestBeanPostProcessor 类实现此接口，**并为其添加 `@Component` 使其可以被扫描**。

   TestBeanPostProcessor.java：

   ```java
   package com.cy.service;
   
   import com.myspring.BeanPostProcessor;
   import com.myspring.Component;
   
   @Component
   public class TestBeanPostProcessor implements BeanPostProcessor {
       @Override
       public Object postProcessBeforeInitialization(Object bean, String beanName) {
           return null;
       }
   
       @Override
       public Object postProcessAfterInitialization(Object bean, String beanName) {
           return null;
       }
   }
   ```

3. 重写实现类中的两个方法。

   TestBeanPostProcessor.java：

   ```java
   package com.cy.service;
   
   import com.myspring.BeanPostProcessor;
   import com.myspring.Component;
   
   @Component("testBeanPostProcessor")
   public class TestBeanPostProcessor implements BeanPostProcessor {
       @Override
       public Object postProcessBeforeInitialization(Object bean, String beanName) {
           if ("userService".equals(beanName)) {
               ((UserServiceImpl) bean).setName("初始化之前改名的userService");
           }
           return bean;
       }
   
       @Override
       public Object postProcessAfterInitialization(Object bean, String beanName) {
           if ("userService".equals(beanName)) {
               ((UserServiceImpl) bean).setName("初始化之后改名的userService");
           }
           return bean;
       }
   }
   ```

4. 在测试类中调用 userService 的 `printBeanName()` 方法，此时的输出结果应该仍为 "userService"。

   Test.java：

   ```java
   package com.cy;
   
   import com.myspring.AnnotationConfigApplicationContext;
   import com.cy.service.UserService;
   
   public class Test {
       public static void main(String[] args) {
           AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
           UserService userService = (UserService) context.getBean("userService");
           // 测试 BeanPostProcessor
           userService.printBeanName();
       }
   }
   ```

### 5.2 功能实现

想要实现这个功能，大致可分为两步：

1. 在扫描时找到所有实现了 BeanPostProcessor 接口的类，将其全部实例化，并放入一个池子中
2. 在每个 Bean 初始化之前，调用池子中所有实现类的 `postProcessBeforeInitialization()` 方法
3. 在每个 Bean 初始化之后，调用池子中所有实现类的 `postProcessAfterInitialization()` 方法

具体的实现步骤如下：

1. 由于这个实现类要在被扫描到后进行解析，因此我们需要在判断一个类是否添加了 `@Component` 注解之后继续判断其是否实现了 BeanPostProcessor 接口。

   AnnotationConfigApplicationContext.java：

   ```java
   // 通过每个类的 Class 对象判断该类是否添加了 @Component 注解
   if (clazz.isAnnotationPresent(Component.class)) {
       // 判断这个类是否实现了 BeanPostProcessor 接口
       if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
       }
       // 获取类的 @Component 注解
       // ... ...
   }
   ```

2. 如果实现了这个接口，我们就将其实例化。

   AnnotationConfigApplicationContext.java：

   ```java
   // 通过每个类的 Class 对象判断该类是否添加了 @Component 注解
   if (clazz.isAnnotationPresent(Component.class)) {
       // 判断这个类是否实现了 BeanPostProcessor 接口
       if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
           // 如果实现了 BeanPostProcessor 接口，就直接实例化
           BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
       }
       // 获取类的 @Component 注解
       // ... ...
   }
   ```

3. 实例化之后，我们就可以把它存到一个池子里，这里我们为 AnnotationConfigApplicationContext 添加一个 List 类型的私有属性。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 用于存储 BeanPostProcessor 实现类
    */
   private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
   ```

4. 把刚才实例化的对象存入池子中。

   AnnotationConfigApplicationContext.java：

   ```java
   // 通过每个类的 Class 对象判断该类是否添加了 @Component 注解
   if (clazz.isAnnotationPresent(Component.class)) {
       // 判断这个类是否实现了 BeanPostProcessor 接口
       if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
           // 如果实现了 BeanPostProcessor 接口，就直接实例化
           BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
           // 把实例存入池子中
           beanPostProcessorList.add(instance);
       }
       // 获取类的 @Component 注解
       // ... ...
   }
   ```

   这样一来，扫描时的工作就完成了。接下来，就是在每个 Bean 的初始化之前和之后调用所有实现类的相关方法。

5. 首先，我们要在所有 Bean 初始化之前调用池子中所有实现类的 `postProcessBeforeInitialization()` 方法。

   AnnotationConfigApplicationContext.java：

   ```java
   // ... ...
   // 调用 beanPostProcessorList 中所有实现类的 postProcessBeforeInitialization() 方法
   for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
       bean = beanPostProcessor.postProcessBeforeInitialization(bean, beanName);
   }
   // 判断当前 Bean 是否实现了 InitializeBean 接口（初始化）
   if (bean instanceof InitializeBean) {
       // 如果实现了 InitializeBean 接口，就可以直接强转，然后调用 afterPropertiesSet() 方法
       ((InitializeBean) bean).afterPropertiesSet();
   }
   // 把 Bean 对象返回
   return bean;
   ```

6. 接着，我们只需在所有 Bean 初始化之后调用池子中所有实现类的 `postProcessAfterInitialization()` 方法即可。

   AnnotationConfigApplicationContext.java：

   ```java
   // ... ...
   // 调用 beanPostProcessorList 中所有实现类的 postProcessBeforeInitialization() 方法
   for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
       bean = beanPostProcessor.postProcessBeforeInitialization(bean, beanName);
   }
   // 判断当前 Bean 是否实现了 InitializeBean 接口（初始化）
   if (bean instanceof InitializeBean) {
       // 如果实现了 InitializeBean 接口，就可以直接强转，然后调用 afterPropertiesSet() 方法
       ((InitializeBean) bean).afterPropertiesSet();
   }
   // 调用 beanPostProcessorList 中所有实现类的 postProcessAfterInitialization() 方法
   for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
       bean = beanPostProcessor.postProcessAfterInitialization(bean, beanName);
   }
   // 把 Bean 对象返回
   return bean;
   ```

### 5.3 测试结果

写完以上代码，我们重新回到测试类，运行以下代码查看结果：

Test.java：

```java
package com.cy;

import com.myspring.AnnotationConfigApplicationContext;
import com.cy.service.UserService;

public class Test {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        // 测试 BeanPostProcessor
        UserService userService = (UserService) context.getBean("userService");
        // 测试 BeanPostProcessor
        userService.printBeanName();
    }
}
```

输出结果：

```
初始化之前改名的userService被初始化了！
初始化之后改名的userService
```

可以看出，初始化时输出的内容和初始化后输出的内容不同，这说明两个方法都已经成功了。



## 6. 二级缓存

当我们在为 Bean 注入属性时，可能会产生**循环依赖**的问题：假设 A 类中包含 B 类型的属性，B 类中包含 A 类型的属性。那么在创建 A 对象时，就必须先创建 B 对象，这样才能将 B 对象引用到 A 对象的属性中；可是要想创建 B 对象，就必须先创建 A 对象，这样才能将 A 对象引用到 B 对象的属性中。这样一来，就会导致死循环，从而产生 **StackOverflowError 异常**。为了解决这个问题，我们可以引入二级缓存。

### 6.1 准备工作

1. 我们之前已经在 UserServiceImpl 中添加了 OrderService 类型的属性。为了产生循环依赖问题，我们需要在 OrderServiceImpl 中添加一个 UserServiceImpl 类型的属性。

   OrderServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.Component;
   
   @Component("orderService")
   public class OrderServiceImpl implements OrderService {
       @Autowired
       private UserService userService;
   }
   ```

2. 然后，我们需要为 OrderService 添加一个用于输出 UserService 的方法。

   OrderService.java：

   ```java
   package com.cy.service;
   
   public interface OrderService {
       void methodWithUsers();
   }
   ```

   OrderServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.Component;
   
   @Component("orderService")
   public class OrderServiceImpl implements OrderService {
       @Autowired
       private UserService userService;
   
       @Override
       public void methodWithUsers() {
           System.out.println(userService);
       }
   }
   ```

3. 并且，我们需要将 UserServiceImpl 改成单例 Bean。

   UserServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.BeanNameAware;
   import com.myspring.Component;
   import com.myspring.InitializeBean;
   import com.myspring.Scope;
   
   @Component("userService")
   @Scope("singleton")
   public class UserServiceImpl implements UserService, BeanNameAware, InitializeBean {
       @Autowired
       private OrderService orderService;
   
       //... ...
       
       @Override
       public void methodWithOrders() {
           System.out.println(orderService);
       }
   }
   ```

4. 在测试类中编写以下代码，以验证循环依赖问题是否解决。此时未解决循环依赖，因此会引发 **StackOverflowError** 异常。

   Test.java：

   ```java
   package com.cy;
   
   import com.myspring.AnnotationConfigApplicationContext;
   import com.cy.service.OrderService;
   import com.cy.service.UserService;
   
   public class Test {
       public static void main(String[] args) {
           // 测试循环依赖
           AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
           UserService userService = (UserService) context.getBean("userService");
           OrderService orderService = (OrderService) context.getBean("orderService");
           System.out.println(orderService);
           userService.methodWithOrders();
           System.out.println(userService);
           orderService.methodWithUsers();
       }
   }
   ```

### 6.2 功能实现

我们之前写的单例池，便被称为“一级缓存”。而所谓“二级缓存”，便是在一级缓存的基础上，再加入一个单例池，只不过其中存储的不是初始化后的单例 Bean，而是刚刚通过构造器进行实例化，还没有进行属性赋值的半成品单例 Bean。

1. 首先，我们需要创建一个 ConcurrentHashMap 来存储半成品单例 Bean。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 二级缓存
    */
   private ConcurrentHashMap<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
   ```

2. 每当单例 Bean 实例化之后，就应当被放入二级缓存中。因此，我们需要在实例化的代码和属性填充的代码中间执行这一步操作。

   AnnotationConfigApplicationContext.java（只展示关键代码）：

   ```java
   /**
    * 创建 Bean 对象
    * @param beanName 要创建的 Bean 的名称
    * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
    * @return 创建出来的 Bean 对象
    */
   private Object createBean(String beanName, BeanDefinition beanDefinition) {
       // 获取要创建的 Bean 对象的 Class 对象
       Class clazz = beanDefinition.getClazz();
       try {
           // 通过反射创建 Bean 对象
           Object bean = clazz.getDeclaredConstructor().newInstance();
           // 把 Bean 对象放到二级缓存中
           earlySingletonObjects.put(beanName, bean);
           // 遍历该类的全部属性准备进行属性填充
           // ... ...
       }
       // ... ...
   }
   ```

3. 而在属性填充完毕之后，二级缓存就不再发挥任何作用了。因此，可以将 bean 从二级缓存中移除。

   AnnotationConfigApplicationContext.java（只展示关键代码）：

   ```java
   /**
    * 创建 Bean 对象
    * @param beanName 要创建的 Bean 的名称
    * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
    * @return 创建出来的 Bean 对象
    */
   private Object createBean(String beanName, BeanDefinition beanDefinition) {
       // 获取要创建的 Bean 对象的 Class 对象
       Class clazz = beanDefinition.getClazz();
       try {
           // 通过反射创建 Bean 对象
           Object bean = clazz.getDeclaredConstructor().newInstance();
           // 把 Bean 对象放到二级缓存中
           earlySingletonObjects.put(beanName, bean);
           // 遍历该类的全部属性准备进行属性填充
           for (Field declaredField : clazz.getDeclaredFields()) {
               // ... ...
           }
           // 把 Bean 从二级缓存中移除
           earlySingletonObjects.remove(beanName);
           // ... ...
       }
       // ... ...
   }
   ```

4. 接着，我们需要稍微修改一下 `getBean()` 方法验证 Bean 的作用域的方式，将原本的从单例池中获取的判断方式改为通过 beanDefinition 直接进行判断。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 传入字符串参数的 getBean 方法
    * @param beanName 要获取对象的 JavaBean 的名称
    * @return 要取出的 JavaBean 对象
    */
   public Object getBean(String beanName) {
       // 判断传入的参数是否在 beanDefinitionMap 中定义过
       if (beanDefinitionMap.containsKey(beanName)) {
           // 获取 BeanDefinition 对象
           BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
           // 需要判断它的作用域来决定是否创建对象
           if ("singleton".equals(beanDefinition.getScope())) {
               // 单例模式直接取出即可
               Object bean = singletonObjects.get(beanName);
               return bean;
           }
           else {
               // 不在单例池中存在，意味着是原型模式，需要创建对象
               Object bean = createBean(beanName, beanDefinition);
               return bean;
           }
       }
       else {
           // 不存在意味着没有定义，则抛出异常（推荐自定义异常，这里用空指针代替）
           throw new NullPointerException();
       }
   }
   ```

5. 之后，我们还需要修改单例 Bean 在获取 Bean 对象时的代码。如果在一级缓存中找不到，还可以去二级缓存中寻找。

   AnnotationConfigApplicationContext.java：

   ```java
   // 单例模式直接取出即可
   Object bean = singletonObjects.get(beanName);
   // 如果取不到，就尝试从二级缓存中取出
   if (bean == null) {
       bean = earlySingletonObjects.get(beanName);
   }
   return bean;
   ```

6. 如果二级缓存仍然找不到，我们就需要调用 `createBean()` 方法进行创建。

   AnnotationConfigApplicationContext.java：

   ```java
   // 单例模式直接取出即可
   Object bean = singletonObjects.get(beanName);
   // 如果取不到，就尝试从二级缓存中取出
   if (bean == null) {
       bean = earlySingletonObjects.get(beanName);
   }
   // 如果二级缓存取不到，就调用 createBean() 方法创建对象
   if (bean == null) {
       bean = createBean(beanName, beanDefinition);
   }
   return bean;
   ```

7. 除此之外，我们还需要将扫描完成后创建单例 Bean 的代码进行修改，将调用 `createBean()` 方法改为调用 `getBean()` 方法。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 传入 Class 对象的有参构造
    * @param configClass 配置类的 Class 对象
    */
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
       // 扫描配置类中指定的路径
       scan(configClass);
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
               // 把 Bean 对象放入单例池
               singletonObjects.put(beanName, bean);
           }
       }
   }
   ```

8. 并且，将 Bean 对象放入单例池这一步也应该在 `createBean()` 方法中执行。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 传入 Class 对象的有参构造
    * @param configClass 配置类的 Class 对象
    */
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
       // 扫描配置类中指定的路径
       scan(configClass);
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
   ```

   ```java
   /**
    * 创建 Bean 对象
    * @param beanName 要创建的 Bean 的名称
    * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
    * @return 创建出来的 Bean 对象
    */
   private Object createBean(String beanName, BeanDefinition beanDefinition) {
       // 获取要创建的 Bean 对象的 Class 对象
       Class clazz = beanDefinition.getClazz();
       try {
           // 通过反射创建 Bean 对象
           Object bean = clazz.getDeclaredConstructor().newInstance();
           // 把 Bean 对象放到二级缓存中
           earlySingletonObjects.put(beanName, bean);
           // 遍历该类的全部属性准备进行属性填充
           for (Field declaredField : clazz.getDeclaredFields()) {
               // 判断属性上是否存在 @Autowired 注解
               if (declaredField.isAnnotationPresent(Autowired.class)) {
                   // 把属性名作为参数传递到 getBean() 方法中来获取对象
                   Object fieldBean = getBean(declaredField.getName());
                   // 如果获取的对象为 null，且 @Autowired 的 required 属性为 true，就需要抛出一个异常
                   if (fieldBean == null) {
                       Autowired autowiredAnnotation = declaredField.getDeclaredAnnotation(Autowired.class);
                       if (autowiredAnnotation.required()) {
                           throw new NullPointerException();
                       }
                   }
                   // 破坏属性的私有
                   declaredField.setAccessible(true);
                   // 将 fieldBean 注入 bean 的属性值
                   declaredField.set(bean, fieldBean);
               }
           }
           // 把 Bean 从二级缓存中移除
           earlySingletonObjects.remove(beanName);
           // 判断当前 Bean 是否实现了 BeanNameAware 接口
           if (bean instanceof BeanNameAware) {
               // 如果实现了 BeanNameAware 接口，就可以直接强转，然后调用 setName() 方法
               ((BeanNameAware) bean).setName(beanName);
           }
           // 调用 beanPostProcessorList 中所有实现类的 postProcessBeforeInitialization() 方法
           for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
               bean = beanPostProcessor.postProcessBeforeInitialization(bean, beanName);
           }
           // 判断当前 Bean 是否实现了 InitializeBean 接口
           if (bean instanceof InitializeBean) {
               // 如果实现了 InitializeBean 接口，就可以直接强转，然后调用 afterPropertiesSet() 方法
               ((InitializeBean) bean).afterPropertiesSet();
           }
           // 调用 beanPostProcessorList 中所有实现类的 postProcessAfterInitialization() 方法
           for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
               bean = beanPostProcessor.postProcessAfterInitialization(bean, beanName);
           }
           // 把单例的 Bean 对象放入单例池
           if ("singleton".equals(beanDefinition.getScope())) {
               singletonObjects.put(beanName, bean);
           }
           // 把 Bean 对象返回
           return bean;
       }
       catch (InstantiationException e) {
           e.printStackTrace();
       }
       catch (IllegalAccessException e) {
           e.printStackTrace();
       }
       catch (InvocationTargetException e) {
           e.printStackTrace();
       }
       catch (NoSuchMethodException e) {
           e.printStackTrace();
       }
       catch (Exception e) {
           e.printStackTrace();
       }
       return null;
   }
   ```

9. 其实，到此为止二级缓存的功能我们已经实现了。但是，考虑到上面两个方法中的代码有些过于杂乱，因此可以抽象出两个方法来让代码看得更清晰些。

   AnnotationConfigApplicationContext.java：

   ```java
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
   ```

   ```java
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
               // 如果获取的对象为 null，且 @Autowired 的 required 属性为 true，就需要抛出一个异常
               if (fieldBean == null) {
                   Autowired autowiredAnnotation = declaredField.getDeclaredAnnotation(Autowired.class);
                   if (autowiredAnnotation.required()) {
                       throw new NullPointerException();
                   }
               }
               // 破坏属性的私有
               declaredField.setAccessible(true);
               // 将 fieldBean 注入 bean 的属性值
               declaredField.set(bean, fieldBean);
           }
       }
   }
   ```

   抽象出这两个方法后，原本方法的代码变为如下：

   ```java
   /**
    * 传入 Class 对象的有参构造
    * @param configClass 配置类的 Class 对象
    */
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
       // 扫描配置类中指定的路径
       scan(configClass);
       // 创建所有的单例 Bean 并放入单例池中
       createAllSingletons();
   }
   ```

   ```java
   /**
    * 创建 Bean 对象
    * @param beanName 要创建的 Bean 的名称
    * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
    * @return 创建出来的 Bean 对象
    */
   private Object createBean(String beanName, BeanDefinition beanDefinition) {
       // 获取要创建的 Bean 对象的 Class 对象
       Class clazz = beanDefinition.getClazz();
       try {
           // 通过反射创建 Bean 对象
           Object bean = clazz.getDeclaredConstructor().newInstance();
           // 把 Bean 对象放到二级缓存中
           earlySingletonObjects.put(beanName, bean);
           // 填充 Bean 对象的属性
           populateBean(clazz, bean);
           // 把 Bean 从二级缓存中移除
           earlySingletonObjects.remove(beanName);
           // 判断当前 Bean 是否实现了 BeanNameAware 接口
           if (bean instanceof BeanNameAware) {
               // 如果实现了 BeanNameAware 接口，就可以直接强转，然后调用 setName() 方法
               ((BeanNameAware) bean).setName(beanName);
           }
           // 调用 beanPostProcessorList 中所有实现类的 postProcessBeforeInitialization() 方法
           for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
               bean = beanPostProcessor.postProcessBeforeInitialization(bean, beanName);
           }
           // 判断当前 Bean 是否实现了 InitializeBean 接口
           if (bean instanceof InitializeBean) {
               // 如果实现了 InitializeBean 接口，就可以直接强转，然后调用 afterPropertiesSet() 方法
               ((InitializeBean) bean).afterPropertiesSet();
           }
           // 调用 beanPostProcessorList 中所有实现类的 postProcessAfterInitialization() 方法
           for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
               bean = beanPostProcessor.postProcessAfterInitialization(bean, beanName);
           }
           // 把单例的 Bean 对象放入单例池
           if ("singleton".equals(beanDefinition.getScope())) {
               singletonObjects.put(beanName, bean);
           }
           // 把 Bean 对象返回
           return bean;
       }
       catch (InstantiationException e) {
           e.printStackTrace();
       }
       catch (IllegalAccessException e) {
           e.printStackTrace();
       }
       catch (InvocationTargetException e) {
           e.printStackTrace();
       }
       catch (NoSuchMethodException e) {
           e.printStackTrace();
       }
       catch (Exception e) {
           e.printStackTrace();
       }
       return null;
   }
   ```

### 6.3 测试结果

写完以上代码，我们重新回到测试类，运行以下代码查看结果：

Test.java：

```java
package com.cy;

import com.myspring.AnnotationConfigApplicationContext;
import com.cy.service.OrderService;
import com.cy.service.UserService;

public class Test {
    public static void main(String[] args) {
        // 测试循环依赖
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService userService = (UserService) context.getBean("userService");
        OrderService orderService = (OrderService) context.getBean("orderService");
        System.out.println(orderService);
        userService.methodWithOrders();
        System.out.println(userService);
        orderService.methodWithUsers();
    }
}
```

输出结果：

```
com.cy.service.OrderServiceImpl@d716361
com.cy.service.OrderServiceImpl@d716361
com.cy.service.UserServiceImpl@6ff3c5b5
com.cy.service.UserServiceImpl@6ff3c5b5
```

可以看出，两个对象中的属性值分别是另一个对象，这就意味着我们成功解决了循环依赖问题。



## 7. 面向切面编程

接下来，我们就来简单实现一下 Spring 中面向切面编程（即 AOP）的相关功能，这一步骤需要利用前面第 5 步中编写好的后置处理器。等到代码编写完毕后，我们可以通过一个日志类来测试代码是否能够成功运行。

### 7.1 准备工作

1. 在 java/com/myspring 目录下创建一个 `@EnableAspectJAutoProxy` 注解，这便是 Spring 中用来开启 AOP 的注解。

   EnableAspectJAutoProxy.java：

   ```java
   package com.myspring;
   
   import java.lang.annotation.ElementType;
   import java.lang.annotation.Retention;
   import java.lang.annotation.RetentionPolicy;
   import java.lang.annotation.Target;
   
   @Target(ElementType.TYPE)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface EnableAspectJAutoProxy {
   }
   ```

2. 在 java/com/myspring 目录下创建一个 `@Aspect` 注解，用于声明某个类是一个切面。

   Aspect.java：

   ```java
   package com.myspring;
   
   import java.lang.annotation.ElementType;
   import java.lang.annotation.Retention;
   import java.lang.annotation.RetentionPolicy;
   import java.lang.annotation.Target;
   
   @Target(ElementType.TYPE)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Aspect {
   }
   ```

3. 在 java/com/myspring 目录下创建一个 `@Before` 注解，用于声明某个方法是一个前置通知。

   Before.java：

   ```java
   package com.myspring;
   
   import java.lang.annotation.ElementType;
   import java.lang.annotation.Retention;
   import java.lang.annotation.RetentionPolicy;
   import java.lang.annotation.Target;
   
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Before {
       String value();
   }
   ```

4. 在 java/com/myspring 目录下创建一个 `@After` 注解，用于声明某个方法是一个后置通知。

   After.java：

   ```java
   package com.myspring;
   
   import java.lang.annotation.ElementType;
   import java.lang.annotation.Retention;
   import java.lang.annotation.RetentionPolicy;
   import java.lang.annotation.Target;
   
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface After {
       String value();
   }
   ```

5. 在 java/com/myspring 目录下创建一个 AnnotationAwareAspectJAutoProxyCreator 类，令其实现 BeanPostProcessor 接口。

   AnnotationAwareAspectJAutoProxyCreator.java：

   ```java
   package com.myspring;
   
   public class AnnotationAwareAspectJAutoProxyCreator implements BeanPostProcessor {
       @Override
       public Object postProcessBeforeInitialization(Object bean, String beanName) {
           return bean;
       }
   
       @Override
       public Object postProcessAfterInitialization(Object bean, String beanName) {
           return bean;
       }
   }
   ```

6. 在 java/com/cy/service 目录下创建一个 Log 类，并在其中添加刚刚创建的注解，用于实现日志切面。需要注意的是，原版 Spring 中的 `@Before` 和 `@After` 注解中的参数填写的是 execute 表达式，此处为了简便，我们直接填写切入点所在的 BeanName，并在 `.` 号后面附上要作为切入点的方法名。

   Log.java：

   ```java
   package com.cy.service;
   
   import com.myspring.After;
   import com.myspring.Aspect;
   import com.myspring.Before;
   import com.myspring.Component;
   
   @Component("log")
   @Aspect
   public class Log {
       @Before("userService.methodWithOrders")
       public void logBefore() {
           System.out.println("【debug】这是方法执行前的日志输出。");
       }
   
       @After("orderService.methodWithUsers")
       public void logAfter() {
           System.out.println("【debug】这是方法执行后的日志输出。");
       }
   }
   ```

7. 为 AppConfig 添加 `@EnableAspectJAutoProxy` 注解。

   AppConfig.java：

   ```java
   package com.cy;
   
   import com.myspring.ComponentScan;
   import com.myspring.EnableAspectJAutoProxy;
   
   @ComponentScan("com.cy.service")
   @EnableAspectJAutoProxy
   public class AppConfig {
   }
   ```

8. 在测试类中编写以下代码，以验证 AOP 功能是否实现。

   Test.java：

   ```java
   package com.cy;
   
   import com.myspring.AnnotationConfigApplicationContext;
   import com.cy.service.OrderService;
   import com.cy.service.UserService;
   
   public class Test {
       public static void main(String[] args) {
           // 测试 AOP
           AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
           UserService userService = (UserService) context.getBean("userService");
           OrderService orderService = (OrderService) context.getBean("orderService");
           userService.methodWithOrders();
           orderService.methodWithUsers();
       }
   }
   ```

### 7.2 功能实现

1. 我们需要在 AnnotationConfigApplicationContext 类的有参构造方法中判断传入的配置类是否添加了 `@EnableAspectJAutoProxy` 注解。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 传入 Class 对象的有参构造
    * @param configClass 配置类的 Class 对象
    */
   public AnnotationConfigApplicationContext(Class configClass) {
       // 接收传入的配置类
       this.configClass = configClass;
       // 判断配置类是否开启 AOP
       checkAop();
       // 扫描配置类中指定的路径
       scan(configClass);
       // 创建所有的单例 Bean 并放入单例池中
       createAllSingletons();
   }
   ```

2. 具体实现 `checkAop()` 方法，判断配置类是否开启了 AOP，如果开启就向 Bean 容器中添加一个 AnnotationAwareAspectJAutoProxyCreator。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 判断配置类是否开启了 AOP，如果开启就向 Bean 容器中添加一个 AnnotationAwareAspectJAutoProxyCreator
    */
   private void checkAop() {
   }
   ```

3. 首先，我们需要判断传入的配置类是否添加了 `@EnableAspectJAutoProxy` 注解。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 判断配置类是否开启了 AOP，如果开启就向 Bean 容器中添加一个 AnnotationAwareAspectJAutoProxyCreator
    */
   private void checkAop() {
       // 判断类上是否有 @EnableAspectJAutoProxy 注解
       if (configClass.isAnnotationPresent(EnableAspectJAutoProxy.class)) {
           
       }
   }
   ```

4. 如果存在此注解，便先获取 AnnotationAwareAspectJAutoProxyCreator 的 Class 对象，为创建其实例做准备。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 判断配置类是否开启了 AOP，如果开启就向 Bean 容器中添加一个 AnnotationAwareAspectJAutoProxyCreator
    */
   private void checkAop() {
       // 判断类上是否有 @EnableAspectJAutoProxy 注解
       if (configClass.isAnnotationPresent(EnableAspectJAutoProxy.class)) {
           // 如果存在此注解，便先获取 AnnotationAwareAspectJAutoProxyCreator 的 Class 对象
           Class clazz = AnnotationAwareAspectJAutoProxyCreator.class;
       }
   }
   ```

5. 通过 Class 对象将 AnnotationAwareAspectJAutoProxyCreator 实例化。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 判断配置类是否开启了 AOP，如果开启就向 Bean 容器中添加一个 AnnotationAwareAspectJAutoProxyCreator
    */
   private void checkAop() {
       // 判断类上是否有 @EnableAspectJAutoProxy 注解
       if (configClass.isAnnotationPresent(EnableAspectJAutoProxy.class)) {
           // 如果存在此注解，便先获取 AnnotationAwareAspectJAutoProxyCreator 的 Class 对象
           Class clazz = AnnotationAwareAspectJAutoProxyCreator.class;
           try {
               // 将 AnnotationAwareAspectJAutoProxyCreator 实例化
               BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
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
   ```

6. 将创建出的实例添加到 beanPostProcessorList 中，以待后续扫描组件时进行 AOP 操作。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 判断配置类是否开启了 AOP，如果开启就向 Bean 容器中添加一个 AnnotationAwareAspectJAutoProxyCreator
    */
   private void checkAop() {
       // 判断类上是否有 @EnableAspectJAutoProxy 注解
       if (configClass.isAnnotationPresent(EnableAspectJAutoProxy.class)) {
           // 如果存在此注解，便先获取 AnnotationAwareAspectJAutoProxyCreator 的 Class 对象
           Class clazz = AnnotationAwareAspectJAutoProxyCreator.class;
           try {
               // 将 AnnotationAwareAspectJAutoProxyCreator 实例化
               BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
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
   ```

7. 接下来，我们需要在扫描组件的过程中对添加了 `@Aspect` 注解的组件进行特殊处理。因此，我们需要在判断一个类是否添加了 `@Component` 注解后编写这部分代码。由于当前这段代码过于冗长，因此我们先将已经写好的内容提取为两个方法：

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 对配置类中指定的路径进行扫描
    * @param configClass 配置类的 Class 对象
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
                   // 判断文件是不是类文件
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
                               // 创建当前类的 BeanDefinition 对象并添加到 map 中
                               putInBeanDefinitionMap(clazz);
                           }
                       }
                       catch (ClassNotFoundException e) {
                           e.printStackTrace();
                       }
                       catch (NoSuchMethodException e) {
                           e.printStackTrace();
                       }
                       catch (IllegalAccessException e) {
                           e.printStackTrace();
                       }
                       catch (InstantiationException e) {
                           e.printStackTrace();
                       }
                       catch (InvocationTargetException e) {
                           e.printStackTrace();
                       }
                   }
               }
           }
       }
   }
   ```

   ```java
   /**
    * 判断传入的 Class 对象是否实现了 BeanPostProcessor 接口，如果实现了就创建实例存储到 list 中
    * @param clazz 扫描到的类的 Class 对象
    * @throws InstantiationException
    * @throws IllegalAccessException
    * @throws InvocationTargetException
    * @throws NoSuchMethodException
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
   ```

   ```java
   /**
    * 根据传入的 CLass 对象创建 BeanDefinition 对象并存入 Map 中
    * @param clazz
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
       }
       else {
           // 没有配置 @Scope 注解的默认是单例模式
           beanDefinition.setScope("singleton");
       }
       // 把 BeanDefinition 对象放入 Map
       beanDefinitionMap.put(beanName, beanDefinition);
   }
   ```

8. 然后我们便可以在两个方法的中间加入一个新的方法，来判断这个类是否为切面类，并对切面类执行后续的解析操作。

   AnnotationConfigApplicationContext.java：

   ```java
   // ... ...
   // 通过每个类的 Class 对象判断该类是否添加了 @Component 注解
   if (clazz.isAnnotationPresent(Component.class)) {
       // 把实现了 BeanPostProcessor 接口的类实例添加到 list 中
       addToBeanPostProcessorList(clazz);
       // 对添加了 @Aspect 注解的类执行额外操作
       getPointcutFromAspect(clazz);
       // 创建当前类的 BeanDefinition 对象并添加到 map 中
       putInBeanDefinitionMap(clazz);
   }
   // ... ...
   ```

   ```java
   /**
    * 判断传入的类是否是切面类，如果是切面类就进行解析
    * @param clazz
    */
   private void getPointcutFromAspect(Class<?> clazz) {
   }
   ```

9. 首先要判断这个类是否添加了 `@Aspect` 注解。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 判断传入的类是否是切面类，如果是切面类就进行解析
    * @param clazz
    */
   private void getPointcutFromAspect(Class<?> clazz) {
       // 判断是否添加了 @Aspect 注解
       if (clazz.isAnnotationPresent(Aspect.class)) {
           
       }
   }
   ```

10. 对于添加了 `@Aspect` 注解的类，我们要遍历它的全部方法。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 判断传入的类是否是切面类，如果是切面类就进行解析
     * @param clazz
     */
    private void getPointcutFromAspect(Class<?> clazz) {
        // 判断是否添加了 @Aspect 注解
        if (clazz.isAnnotationPresent(Aspect.class)) {
            // 遍历这个类的全部方法
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
    
            }
        }
    }
    ```

11. 对于这些方法，我们需要依次去判断它们是否添加了 `@Before` 注解或者 `@After` 注解。

    AnnotationConfigApplicationContext.java：

    ```java
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
    
                }
                // 判断是否添加了 @After 注解
                if (declaredMethod.isAnnotationPresent(After.class)) {
                    
                }
            }
        }
    }
    ```

12. 如果添加了这些注解，我们就要去获取注解中的参数。

    AnnotationConfigApplicationContext.java：

    ```java
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
                }
                // 判断是否添加了 @After 注解
                if (declaredMethod.isAnnotationPresent(After.class)) {
                    // 获取 @After 注解
                    After afterAnnotation = declaredMethod.getDeclaredAnnotation(After.class);
                    // 获取注解中的参数
                    String pointcut = afterAnnotation.value();
                }
            }
        }
    }
    ```

13. 接下来，我们就要解析参数了。但是在此之前，我们需要先封装一个类，用来存储需要的数据。

    MethodWithClass.java：

    ```java
    package com.myspring;
    
    import java.lang.reflect.Method;
    
    /**
     * 封装通知方法及其所属的类对象
     */
    public class MethodWithClass {
        private Class clazz;
        private Method method;
    
        public MethodWithClass(Class clazz, Method method) {
            this.clazz = clazz;
            this.method = method;
        }
    
        public Class getClazz() {
            return clazz;
        }
    
        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }
    
        public Method getMethod() {
            return method;
        }
    
        public void setMethod(Method method) {
            this.method = method;
        }
    
        @Override
        public String toString() {
            return "MethodWithClass{" +
                    "clazz=" + clazz +
                    ", method=" + method +
                    '}';
        }
    }
    ```

14. 然后，我们需要创建两个 Map，用于存储这些解析出的切入点和通知。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 用于存储通过 @Before 注解解析出的切入点及通知
     */
    private ConcurrentHashMap<String, List<MethodWithClass>> beforeMethodMap = new ConcurrentHashMap<>();
    
    /**
     * 用于存储通过 @After 注解解析出的切入点及通知
     */
    private ConcurrentHashMap<String, List<MethodWithClass>> afterMethodMap = new ConcurrentHashMap<>();
    ```

15. 这样一来，我们就可以回到 `getPointCutFromAspect()` 方法中，将参数及方法填入对应的 Map 中了。

    AnnotationConfigApplicationContext.java：

    ```java
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
    ```

16. 至此，扫描时要做的工作就已经全部完成了。接下来，就需要在 AnnotationAwareAspectJAutoProxyCreator 类中编写动态代理相关的代码，来真正实现 AOP 的相关功能。由于 AOP 发生在 bean 的初始化后，因此我们只需要重写 `postProcessAfterInitialization()` 方法即可。在这里，我们首先要做的第一步，就是通过 beanName 判断当前 bean 是否要进行 AOP 操作。于是，我们就发现了前面编写的代码的疏漏之处：在 AnnotationAwareAspectJAutoProxyCreator 中无法获取那两个用来存储切入点及通知的 Map。因此，我们需要先为其添加两个属性。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
    package com.myspring;
    
    import java.lang.reflect.Method;
    import java.util.LinkedList;
    import java.util.concurrent.ConcurrentHashMap;
    
    public class AnnotationAwareAspectJAutoProxyCreator implements BeanPostProcessor {
        /**
         * 用于存储通过 @Before 注解解析出的切入点及通知
         */
        private ConcurrentHashMap<String, List<MethodWithClass>> beforeMethodMap = new ConcurrentHashMap<>();
    
        /**
         * 用于存储通过 @After 注解解析出的切入点及通知
         */
        private ConcurrentHashMap<String, List<MethodWithClass>> afterMethodMap = new ConcurrentHashMap<>();
    
        public void setBeforeMethodMap(ConcurrentHashMap<String, List<MethodWithClass>> beforeMethodMap) {
            this.beforeMethodMap = beforeMethodMap;
        }
    
        public void setAfterMethodMap(ConcurrentHashMap<String, List<MethodWithClass>> afterMethodMap) {
            this.afterMethodMap = afterMethodMap;
        }
    
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
            return bean;
        }
    }
    ```

17. 为了让这两个属性中有值，我们还需要调换 AnnotationConfigApplicationContext 的构造方法中 `checkAop()` 方法和 `scan()` 方法的顺序。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 传入 Class 对象的有参构造
     * @param configClass 配置类的 Class 对象
     */
    public AnnotationConfigApplicationContext(Class configClass) {
        // 接收传入的配置类
        this.configClass = configClass;
        // 扫描配置类中指定的路径
        scan(configClass);
        // 判断配置类是否开启 AOP
        checkAop();
        // 创建所有的单例 Bean 并放入单例池中
        createAllSingletons();
    }
    ```

18. 然后，我们需要在 `checkAop()` 方法中为 AnnotationAwareAspectJAutoProxyCreator 对象填充这两个属性。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 判断配置类是否开启了 AOP，如果开启就向 Bean 容器中添加一个 AnnotationAwareAspectJAutoProxyCreator
     */
    private void checkAop() {
        // 判断类上是否有 @EnableAspectJAutoProxy 注解
        if (configClass.isAnnotationPresent(EnableAspectJAutoProxy.class)) {
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
    ```

19. 这样，我们就可以借用两个 map，从而根据 beanName 判断当前 bean 是否要进行 AOP 操作了。以 beforeMethodMap 为例，我们首先要去遍历其中所有的 key，并将 key 拆分为 beanName 和 methodName。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
    /**
     * 想要完成 AOP 操作只需通过此方法对 bean 进行加工即可
     * @param bean  可能需要执行 AOP 操作的 Bean
     * @param beanName  可能需要执行 AOP 操作的 Bean 的 BeanName
     * @return  原本的 bean（无需 AOP） 或加工后的 bean（需要 AOP）
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 判断 beforeMethodMap 中有无传入的 beanName
        for (String key : beforeMethodMap.keySet()) {
            // 把 Map 中的 x.x 格式拆分成 beanName 和 methodName,注意 . 需要转义，否则会当作正则匹配所有
            String[] beanAndMethod = key.split("\\.");
        }
        return bean;
    }
    ```

20. 拆分出的数组的首个元素便是 beanName，如此我们便可以判断是否与传入的 beanName 相同。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 判断 beforeMethodMap 中有无传入的 beanName
        for (String key : beforeMethodMap.keySet()) {
            // 把 Map 中的 x.x 格式拆分成 beanName 和 methodName
            String[] beanAndMethod = key.split("\\.");
            // 判断数组中的 beanName 是否与传入的 beanName 相同
            if (beanAndMethod[0].equals(beanName)) {
                
            }
        }
        return bean;
    }
    ```

21. 我们可以创建一个 List 来存储符合 beanName 的所有 methodName，并在遍历时将对应的 methodName 存入。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 存储该 bean 的前置 methodName
        LinkedList<String> beforeMethods = new LinkedList<>();
        // 判断 beforeMethodMap 中有无传入的 beanName
        for (String key : beforeMethodMap.keySet()) {
            // 把 Map 中的 x.x 格式拆分成 beanName 和 methodName
            String[] beanAndMethod = key.split("\\.");
            // 判断数组中的 beanName 是否与传入的 beanName 相同
            if (beanAndMethod[0].equals(beanName)) {
                // 相同就把 methodName 先存入 List 中等待后面使用
                beforeMethods.add(beanAndMethod[1]);
            }
        }
        return bean;
    }
    ```

22. 对应地，我们可以对 afterMethodMap 做同样的处理。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 存储该 bean 的前置 methodName
        LinkedList<String> beforeMethods = new LinkedList<>();
        // 存储该 bean 的后置 methodName
        LinkedList<String> afterMethods = new LinkedList<>();
        // 判断 beforeMethodMap 中有无传入的 beanName
        for (String key : beforeMethodMap.keySet()) {
            // 把 Map 中的 x.x 格式拆分成 beanName 和 methodName
            String[] beanAndMethod = key.split("\\.");
            // 判断数组中的 beanName 是否与传入的 beanName 相同
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
        return bean;
    }
    ```

23. 为了保证代码清晰，我们可以把上面这一段抽象成一个方法。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
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
        return bean;
    }
    ```

    ```java
    /**
     * 根据传入的 beanName 填充传入的两个方法列表
     * @param beanName  当前 bean 的 beanName
     * @param beforeMethods 要填充的 before 方法列表
     * @param afterMethods  要填充的 after 方法列表
     */
    private void fillMethodList(String beanName, LinkedList<String> beforeMethods, LinkedList<String> afterMethods) {
        // 判断 beforeMethodMap 中有无传入的 beanName
        for (String key : beforeMethodMap.keySet()) {
            // 把 Map 中的 x.x 格式拆分成 beanName 和 methodName
            String[] beanAndMethod = key.split("\\.");
            // 判断数组中的 beanName 是否与传入的 beanName 相同
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
    ```

24. 填充完毕之后，我们便可以根据 list 是否全为空，来判断当前 bean 是否需要创建动态代理对象了。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
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
            
        }
        return bean;
    }
    ```

25. 只要两个方法列表不全为空，就意味着需要创建一个动态代理对象，并在最终直接返回代理对象来代替 bean 对象。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
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
            // 为 bean 创建一个动态代理对象
            Object proxy = Proxy.newProxyInstance();
            // 直接返回代理对象来代替 bean 对象
            return proxy;
        }
        return bean;
    }
    ```

26. 填充创建动态代理对象所需要的参数，其中第三个参数所创建的 InvocationHandler 正是这段代码的核心部分。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
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
            // 为 bean 创建一个动态代理对象
            Object proxy = Proxy.newProxyInstance(bean.getClass().getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return null;
                }
            });
            // 直接返回代理对象来代替 bean 对象
            return proxy;
        }
        return bean;
    }
    ```

27. 在这个 InvocationHandler 中，我们首先需要保证原本的方法可以执行，并返回原本的返回值。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
    // 为 bean 创建一个动态代理对象
    Object proxy = Proxy.newProxyInstance(bean.getClass().getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 执行原本的方法
            Object result = method.invoke(bean, args);
            // 返回原本的返回值
            return result;
        }
    });
    ```

28. 然后我们便可以在执行原本代码之前和之后分别来调用 beforeMethods 和 afterMethods 中的切点所对应的通知了。先以 beforeMethods 为例，我们应当先判断当前方法是否在此列表中。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 判断当前方法是否存在于 beforeMethods 列表中
        if (beforeMethods.contains(method.getName())) {
            
        }
        // 执行原本的方法
        Object result = method.invoke(bean, args);
        // 返回原本的返回值
        return result;
    }
    ```

29. 如果存在，那么就拼接 beanName、`.` 符合和当前方法的名字成一个字符串。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 判断当前方法是否存在于 beforeMethods 列表中
        if (beforeMethods.contains(method.getName())) {
            // 拼接 beanName 和 methodName
            String theKey = new StringBuilder(beanName).append('.').append(method.getName()).toString();
        }
        // 执行原本的方法
        Object result = method.invoke(bean, args);
        // 返回原本的返回值
        return result;
    }
    ```

30. 判断 beforeMethodMap 中是否存在相符的 key。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 判断当前方法是否存在于 beforeMethods 列表中
        if (beforeMethods.contains(method.getName())) {
            // 拼接 beanName 和 methodName
            String theKey = new StringBuilder(beanName).append('.').append(method.getName()).toString();
            // 判断 beforeMethodMap 中是否存在相符的 key
            if (beforeMethodMap.containsKey(theKey)) {
    
            }
        }
        // 执行原本的方法
        Object result = method.invoke(bean, args);
        // 返回原本的返回值
        return result;
    }
    ```

31. 如果找到，就要获取对应 List 中的所有 MethodWithClass 对象。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
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
            }
        }
        // 执行原本的方法
        Object result = method.invoke(bean, args);
        // 返回原本的返回值
        return result;
    }
    ```

32. 依次调用这些所有的方法。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
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
        // 返回原本的返回值
        return result;
    }
    ```

33. 对应地，对 after 的方法做出同样的操作。

    AnnotationAwareAspectJAutoProxyCreator.java：

    ```java
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
    ```

### 7.3 测试结果

写完以上代码，我们重新回到测试类，运行以下代码查看结果：

Test.java：

```java
package com.cy;

import com.myspring.AnnotationConfigApplicationContext;
import com.cy.service.OrderService;
import com.cy.service.UserService;

public class Test {
    public static void main(String[] args) {
        // 测试 AOP
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService userService = (UserService) context.getBean("userService");
        OrderService orderService = (OrderService) context.getBean("orderService");
        userService.methodWithOrders();
        orderService.methodWithUsers();
    }
}
```

输出结果：

```
【debug】这是方法执行前的日志输出。
com.cy.service.OrderServiceImpl@2c7b84de
com.cy.service.UserServiceImpl@3fee733d
【debug】这是方法执行后的日志输出。
```

可以看出，两个方法都在对应的位置输出了日志，这就意味着我们的 AOP 操作成功了。



## 8. 三级缓存

之前我们通过二级缓存成功解决了循环依赖问题，UserService 与 OrderService 的互相引用不会再导致栈溢出问题。然而，伴随着 AOP 功能的实现，新的循环依赖问题又产生了。只不过这一次不是栈溢出，而是引用不完整。不难发现，在我们前面实现的功能中，AOP 是通过后置处理器完成的，步骤在初始化之后。这样一来，两个循环依赖的 bean 实例，其中必然有一个的属性引用的会是执行 AOP 操作前生成的对象，因为二级缓存中存储的是尚未通过后置处理器进行加工的原始对象，而非动态代理生成的代理对象。因此，想要解决这个新问题，仅凭二级缓存是不够的，我们还需要引入三级缓存。通过三级缓存，我们就可以提前对 Bean 对象执行 AOP 操作，以此来解决受到 AOP 影响下的循环依赖的问题。

### 8.1 准备工作

1. 想要解决这个问题，我们首先需要在 java/com/myspring 目录创建一个函数式接口，取名为 ObjectFactory。这个接口存在的意义，是为了延迟后面一个关键方法的执行。

   ObjectFactory.java：

   ```java
   package com.myspring;
   
   @FunctionalInterface
   public interface ObjectFactory {
       Object getObject();
   }
   ```

2. 接下来，我们需要修改两个 Service 接口，将其中最重要的两个方法的返回值修改为另一个接口类型。

   UserService.java：

   ```java
   package com.cy.service;
   
   public interface UserService {
       // ... ...
       OrderService methodWithOrders();
       // ... ...
   }
   ```

   OrderService.java：

   ```java
   package com.cy.service;
   
   public interface OrderService {
       UserService methodWithUsers();
   }
   ```

3. 这样一来，两个接口的实现类也要随之修改。

   UserServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.BeanNameAware;
   import com.myspring.Component;
   import com.myspring.InitializeBean;
   import com.myspring.Scope;
   
   @Component("userService")
   @Scope("singleton")
   public class UserServiceImpl implements UserService, BeanNameAware, InitializeBean {
       @Autowired
       private OrderService orderService;
   
       // ... ...
   
       @Override
       public OrderService methodWithOrders() {
           return orderService;
       }
   }
   ```

   OrderServiceImpl.java：

   ```java
   package com.cy.service;
   
   import com.myspring.Autowired;
   import com.myspring.Component;
   
   @Component("orderService")
   public class OrderServiceImpl implements OrderService {
       @Autowired
       private UserService userService;
   
       @Override
       public UserService methodWithUsers() {
           return userService;
       }
   }
   ```

4. 以上修改全部完毕后，我们便可以按照如下方式编写测试类。

   Test.java：

   ```java
   package com.cy;
   
   import com.myspring.AnnotationConfigApplicationContext;
   import com.cy.service.OrderService;
   import com.cy.service.UserService;
   
   public class Test {
       public static void main(String[] args) {
           // 测试 AOP 对循环依赖的影响
           AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
           UserService userService = (UserService) context.getBean("userService");
           OrderService orderService = (OrderService) context.getBean("orderService");
           userService.methodWithOrders().methodWithUsers();
           orderService.methodWithUsers().methodWithOrders();
       }
   }
   ```

   在我们的预期中，运行这个测试类，得到的应该是如下输出：

   ```
   【debug】这是方法执行前的日志输出。
   【debug】这是方法执行后的日志输出。
   【debug】这是方法执行后的日志输出。
   【debug】这是方法执行前的日志输出。
   ```

   但实际上，如果直接运行这个测试类，得到的却会是如下输出：

   ```
   【debug】这是方法执行前的日志输出。
   【debug】这是方法执行后的日志输出。
   【debug】这是方法执行前的日志输出。
   ```

那么问题究竟出在哪里了呢？为什么输出的日志会少了一句？

问题就出在 `userService.methodWithOrders().methodWithUsers()` 一句，通过 `userService.methodWithOrders()` 可以得到 UserService 中依赖的 OrderService 对象，而这个对象是通过二级缓存得到的，并没有执行 AOP 操作，因此是原始对象而非代理对象。这样一来，这个对象所调用的 `methodWithUsers()` 方法自然不会打印日志。

我们接下来要做的，便是解决这个问题。

### 8.2 功能实现

1. 既然我们想要通过三级缓存来解决问题，那么首要任务就是在 AnnotationConfigApplicationContext 中创建一个三级缓存。我们采用 ConcurrentHashMap 作为装载其的数据结构，两个参数类型分别设置为 String 和前面刚刚创建的函数式接口 ObjectFactory。这样，我们便可以向其中存入 Lambda 表达式了。

   AnnotationConfigApplicationContext.java：

   ```Java
   /**
    * 三级缓存
    */
   private ConcurrentHashMap<String, ObjectFactory> singletonFactories = new ConcurrentHashMap<>();
   ```

2. 接下来，我们要创建一个方法，取名为 `getEarlyBeanReference()`。创建这个方法的目的，是为了判断传入 bean 是否需要 AOP，如果需要就返回其代理对象，否则返回原始对象。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 判断传入的参数 bean 是否需要 AOP，如果需要就返回代理对象，否则返回原始对象
    * @param beanName  传入 bean 的 BeanName
    * @param bean  传入待判断的 bean 对象
    * @return  根据传入参数 bean 对象来返回原始对象或代理对象
    */
   private Object getEarlyBeanReference(String beanName, Object bean) {
       // 创建一个额外的引用用于最终的返回
       Object exposedObject = bean;
       // 判断逻辑
       // ...
       return exposedObject;
   }
   ```

3. 事实上，关于第二步中提到的判断，我们已经通过 AnnotationAwareAspectJAutoProxyCreator 完成了，因此我们只需要调用其中的后置处理方法，即可自动返回原始对象或代理对象。换句话说，我们在这个方法中，仅需找到那个 AnnotationAwareAspectJAutoProxyCreator 的实例即可。首先，我们要去遍历 beanPostProcessorList，并逐个判断其是否为 AnnotationAwareAspectJAutoProxyCreator 的实例。

   AnnotationConfigApplicationContext.java：

   ```java
   private Object getEarlyBeanReference(String beanName, Object bean) {
       // 创建一个额外的引用用于最终的返回
       Object exposedObject = bean;
       // 遍历 beanPostProcessorList
       for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
           // 判断其是否为 AnnotationAwareAspectJAutoProxyCreator 的实例
           if (beanPostProcessor instanceof AnnotationAwareAspectJAutoProxyCreator) {
               
           }
       }
       return exposedObject;
   }
   ```

4. 如果是，就调用其中的后置处理方法，对传入的 bean 进行加工，或者直接返回原始对象。

   AnnotationConfigApplicationContext.java：

   ```java
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
   ```

5. 完成以上操作之后，我们还需要创建一个 List，用以存储正在创建中的 bean 的名称。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 正在创建中的 Bean 的名称
    */
   private LinkedList<String> creatingBeanNameList = new LinkedList<>();
   ```

6. 接下来，我们就要回到 `createBean()` 方法中。在 bean 实例化后，我们就要将 beanName 放入 List 中；而当 bean 创建结束后，就要立即从 List 中删除对应的 beanName。

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 创建 Bean 对象
    * @param beanName 要创建的 Bean 的名称
    * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
    * @return 创建出来的 Bean 对象
    */
   private Object createBean(String beanName, BeanDefinition beanDefinition) {
       // 获取要创建的 Bean 对象的 Class 对象
       Class clazz = beanDefinition.getClazz();
       try {
           // 通过反射创建 Bean 的实例
           Object bean = clazz.getDeclaredConstructor().newInstance();
           // 把 Bean 对象的名称放入 creatingBeanNameList 中
           creatingBeanNameList.add(beanName);
           // ... ...
           // 从 creatingBeanNameList 中删除 BeanName
           creatingBeanNameList.remove(beanName);
           // 把 Bean 对象返回
           return bean;
       }
       // ... ...
   }
   ```

7. 此时，代码中采用的还是二级缓存来避免循环依赖问题。现在，我们需要将其更换为三级缓存。

   AnnotationConfigApplicationContext.java：

   ```java
   private Object createBean(String beanName, BeanDefinition beanDefinition) {
       // 获取要创建的 Bean 对象的 Class 对象
       Class clazz = beanDefinition.getClazz();
       try {
           // 通过反射创建 Bean 的实例
           Object bean = clazz.getDeclaredConstructor().newInstance();
           // 把 Bean 对象的名称放入 creatingBeanNameList 中
           creatingBeanNameList.add(beanName);
           // 把 Bean 对象及其名称放入三级缓存
           Object finalBean = bean;
           singletonFactories.put(beanName, () -> getEarlyBeanReference(beanName, finalBean));
           // 填充 Bean 对象的属性
           populateBean(clazz, bean);
           // ... ...
       }
       // ... ...
   }
   ```

8. 三级缓存存储完毕后，我们就要修改 `getBean()` 方法了。此时这个方法还只会寻找一级缓存和二级缓存，我们需要让其能够查询三级缓存。当然，在此之前，我们还需要把前面创建的 creatingBeanNameList 利用起来。对于正在创建中的 Bean，我们才需要去查询二级缓存和三级缓存；而对于没有在创建中的 Bean，如果一级缓存中不存在，那就直接创建吧！

   AnnotationConfigApplicationContext.java：

   ```java
   /**
    * 传入字符串参数的 getBean 方法
    * @param beanName 要获取对象的 JavaBean 的名称
    * @return 要取出的 JavaBean 对象
    */
   public Object getBean(String beanName) {
       // 判断传入的参数是否在 beanDefinitionMap 中定义过
       if (beanDefinitionMap.containsKey(beanName)) {
           // 获取 BeanDefinition 对象
           BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
           // 需要判断它的作用域来决定是否创建对象
           if ("singleton".equals(beanDefinition.getScope())) {
               // 单例模式直接取出即可
               Object bean = singletonObjects.get(beanName);
               // 如果取不到，且这个 Bean 正在创建，就尝试从二级缓存中取出
               if (bean == null && creatingBeanNameList.contains(beanName)) {
                   bean = earlySingletonObjects.get(beanName);
               }
               // 否则直接调用 createBean() 方法创建对象
               if (bean == null) {
                   bean = createBean(beanName, beanDefinition);
               }
               return bean;
           }
           else {
               // 不在单例池中存在，意味着是原型模式，需要创建对象
               Object bean = createBean(beanName, beanDefinition);
               return bean;
           }
       }
       else {
           // 不存在意味着没有定义，则抛出异常（推荐自定义异常，这里用空指针代替）
           throw new NullPointerException();
       }
   }
   ```

9. 对于从二级缓存中仍然取不出的，我们就要从三级缓存中去取，此时调用 `getObject()` 方法便会自动执行 `getEarlyBeanReference()` 方法。

   AnnotationConfigApplicationContext.java：

   ```java
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
   }
   // 否则直接调用 createBean() 方法创建对象
   if (bean == null) {
       bean = createBean(beanName, beanDefinition);
   }
   return bean;
   ```

10. 取出来之后，我们需要将它放入二级缓存，并从三级缓存中删去。

    AnnotationConfigApplicationContext.java：

    ```java
    // 如果仍然取不到，且这个 Bean 正在创建，就从三级缓存中去取，并放入二级缓存
    if (bean == null && creatingBeanNameList.contains(beanName)) {
        // 从三级缓存中取出，调用 getObject() 方法来执行 Lambda 表达式
        bean = singletonFactories.get(beanName).getObject();
        // 放入二级缓存
        earlySingletonObjects.put(beanName, bean);
        // 从三级缓存中删除
        singletonFactories.remove(beanName);
    }
    ```

11. 除了在此处将其从三级缓存中移除以外，`createBean()` 方法中我们同样也要执行此操作。因为，我们将每个对象都放入了三级缓存，但并不是所有的 Bean 对象都需要提前 AOP。

    AnnotationConfigApplicationContext.java：

    ```java
    /**
     * 创建 Bean 对象
     * @param beanName 要创建的 Bean 的名称
     * @param beanDefinition 根据 Bean 定义来创建 Bean 对象
     * @return 创建出来的 Bean 对象
     */
    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        // 获取要创建的 Bean 对象的 Class 对象
        Class clazz = beanDefinition.getClazz();
        try {
            // 通过反射创建 Bean 的实例
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
            // ... ...
        }
        // ... ...
    }
    ```

12. 此时，如果该 Bean 对象已经提前 AOP 了，那么二级缓存中一定存在它的代理对象。因此，我们需要判断二级缓存中是否存在这样一个对象。如果存在，我们就需要让 bean 去指向那个代理对象。当然，这一步判断应该放在最后，因为接下来我们还需要对原始对象做一系列处理。

    AnnotationConfigApplicationContext.java：

    ```java
    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        // 获取要创建的 Bean 对象的 Class 对象
        Class clazz = beanDefinition.getClazz();
        try {
            // 通过反射创建 Bean 的实例
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
            // ... ...
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
            // 把 Bean 对象返回
            return bean;
        }
        // ... ...
    }
    ```

### 8.3 测试结果

写完以上代码，我们重新回到测试类，运行以下代码查看结果：

Test.java：

```java
package com.cy;

import com.myspring.AnnotationConfigApplicationContext;
import com.cy.service.OrderService;
import com.cy.service.UserService;

public class Test {
    public static void main(String[] args) {
        // 测试 AOP 对循环依赖的影响
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService userService = (UserService) context.getBean("userService");
        OrderService orderService = (OrderService) context.getBean("orderService");
        userService.methodWithOrders().methodWithUsers();
        orderService.methodWithUsers().methodWithOrders();
    }
}
```

输出结果：

```
【debug】这是方法执行前的日志输出。
【debug】这是方法执行后的日志输出。
【debug】这是方法执行后的日志输出。
【debug】这是方法执行前的日志输出。
```

由此可见，伴随着 AOP 功能而产生的新的循环依赖问题就这样被我们通过三级缓存解决了！