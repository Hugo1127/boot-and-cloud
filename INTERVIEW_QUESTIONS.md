# Boot&Cloud 面试题总结

> 本文档按模块分类整理 Java 后端高频面试题，包含标准应答、原理延伸和代码示例，贴合大厂面试风格，可直接用于面试背诵与讲解。

---

## 目录

1. [IOC 容器模块](#一-ioc 容器模块)
2. [AOP 模块](#二-aop 模块)
3. [自动配置模块](#三-自动配置模块)
4. [服务注册与发现模块](#四-服务注册与发现模块)
5. [远程调用模块](#五-远程调用模块)
6. [负载均衡模块](#六-负载均衡模块)
7. [服务熔断与降级模块](#七-服务熔断与降级模块)
8. [JVM 调优模块](#八-jvm 调优模块)
9. [并发编程模块](#九-并发编程模块)
10. [综合设计题](#十-综合设计题)

---

## 一、IOC 容器模块

### 面试题 1：控制反转（IOC）与依赖注入（DI）的区别？

**标准应答**：

控制反转（Inversion of Control）是一种**设计思想**，核心是将对象的创建和控制权从代码中剥离，交给外部容器管理。

依赖注入（Dependency Injection）是 IOC 的**具体实现方式**，通过构造器、Setter 方法或字段注入，将依赖对象注入到目标类中。

**关系**：IOC 是思想，DI 是实现。就像"接口编程"是思想，"抽象类/接口"是实现。

**代码示例**：
```java
// ❌ 没有 IOC：手动创建依赖
public class UserService {
    private UserRepository repository = new UserRepository();
}

// ✅ 使用 IOC：容器管理依赖
@Component
public class UserService {
    @Autowired  // DI 实现
    private UserRepository repository;
}
```

**原理延伸**：
- IOC 的好处：解耦、可测试、可维护
- Spring 通过 BeanFactory 管理 Bean 生命周期
- 本项目实现：[BeanFactory](file:///d:/Work/Java/Boot&Cloud/mini-spring-core/src/main/java/com/bootcloud/core/factory/BeanFactory.java), [DefaultListableBeanFactory](file:///d:/Work/Java/Boot&Cloud/mini-spring-core/src/main/java/com/bootcloud/core/factory/DefaultListableBeanFactory.java)

---

### 面试题 2：Bean 的生命周期是怎样的？

**标准应答**：

Bean 的生命周期包含 4 个阶段：

1. **实例化（Instantiation）** - 通过反射创建 Bean 实例
2. **属性填充（Populate）** - 注入依赖（@Autowired）
3. **初始化（Initialization）** - 执行@PostConstruct 方法
4. **销毁（Destruction）** - 执行@PreDestroy 方法

**记忆口诀**："实 - 属 - 初 - 销"

**详细流程**：
```
1. 扫描@Component 注解 → 生成 BeanDefinition
2. 反射创建实例 → 放入一级缓存
3. 属性注入 → @Autowired 字段注入
4. 执行 Aware 接口回调（如 ApplicationContextAware）
5. 执行 BeanPostProcessor 前置处理
6. 执行@PostConstruct 初始化方法
7. 执行 BeanPostProcessor 后置处理（AOP 代理在此）
8. Bean 就绪，放入单例池
9. 容器关闭时 → 执行@PreDestroy 销毁
```

**代码示例**：
```java
@Component
public class UserService {
    
    @PostConstruct
    public void init() {
        System.out.println("UserService 初始化");
    }
    
    @PreDestroy
    public void destroy() {
        System.out.println("UserService 销毁");
    }
}
```

**原理延伸**：
- 本项目实现：[GenericApplicationContext.refresh()](file:///d:/Work/Java/Boot&Cloud/mini-spring-core/src/main/java/com/bootcloud/core/context/support/GenericApplicationContext.java)
- AOP 代理在初始化后执行（BeanPostProcessor）
- 原型 Bean（@Scope("prototype"）没有销毁阶段

---

### 面试题 3：循环依赖如何解决？三级缓存的作用？

**标准应答**：

循环依赖指：A 依赖 B，B 又依赖 A，形成环路。

Spring 通过**三级缓存**解决**单例 Bean**的**字段注入**循环依赖。

**三级缓存结构**：
```java
// 一级缓存：完全初始化好的 Bean
Map<String, Object> singletonObjects;

// 二级缓存：早期暴露的 Bean（未完全初始化）
Map<String, Object> earlySingletonObjects;

// 三级缓存：Bean 工厂，用于创建代理对象
Map<String, ObjectFactory<?>> singletonFactories;
```

**解决流程**（A→B→A 循环）：
```
1. A 实例化 → 放入三级缓存（工厂）
2. A 注入 B → 发现 B 不存在
3. B 实例化 → 放入三级缓存
4. B 注入 A → 从三级缓存获取 A 的工厂 → 创建 A 的早期引用 → 放入二级缓存
5. B 初始化完成 → 放入一级缓存
6. A 注入 B → 从一级缓存获取 B
7. A 初始化完成 → 放入一级缓存
```

**关键点**：
- 三级缓存的工厂可以提前暴露 Bean 引用
- 二级缓存缓存早期引用，避免重复创建
- 一级缓存缓存最终 Bean

**不生效场景**：
- ❌ 构造器注入（无法提前暴露）
- ❌ 原型 Bean（不缓存）

**原理延伸**：
- 本项目实现：[DefaultListableBeanFactory](file:///d:/Work/Java/Boot&Cloud/mini-spring-core/src/main/java/com/bootcloud/core/factory/DefaultListableBeanFactory.java)
- 面试加分项：说明 AOP 代理时三级缓存的作用

---

### 面试题 4：@Autowired 的注入原理？

**标准应答**：

@Autowired 通过**反射 + 类型匹配**实现依赖注入。

**注入流程**：
```
1. 扫描所有字段，查找@Autowired 注解
2. 获取字段的 Class 类型
3. 从 BeanFactory 查找匹配的 Bean
4. 设置字段可访问（setAccessible(true)）
5. 通过反射设置字段值（field.set()）
```

**匹配规则**：
1. 优先按类型匹配（byType）
2. 如果多个匹配，按字段名匹配（byName）
3. 仍无法确定，抛出 NoUniqueBeanDefinitionException

**代码示例**：
```java
// 源码位置：DefaultListableBeanFactory.doAutowiredFields()
for (Field field : beanClass.getDeclaredFields()) {
    if (field.isAnnotationPresent(Autowired.class)) {
        Class<?> fieldType = field.getType();
        Object bean = getBean(fieldType);  // 按类型查找
        field.setAccessible(true);
        field.set(instance, bean);  // 反射注入
    }
}
```

**原理延伸**：
- @Qualifier 可指定 Bean 名称
- @Primary 可标注首选 Bean
- 构造器注入性能优于字段注入

---

## 二、AOP 模块

### 面试题 5：JDK 动态代理和 CGLIB 代理的区别？

**标准应答**：

| 对比项 | JDK 动态代理 | CGLIB 代理 |
|--------|------------|-----------|
| **实现方式** | 基于接口 | 基于继承 |
| **代理对象** | 实现接口的类 | 目标类的子类 |
| **性能** | 较快（JDK 原生） | 稍慢（字节码生成） |
| **限制** | 必须有接口 | 不能代理 final 类 |
| **Spring 选择** | 有接口用 JDK | 无接口用 CGLIB |

**代码示例**：
```java
// JDK 动态代理
UserService proxy = (UserService) Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    (proxy, method, args) -> {
        System.out.println("前置通知");
        Object result = method.invoke(target, args);
        System.out.println("后置通知");
        return result;
    }
);

// CGLIB 代理
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(UserService.class);
enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
    System.out.println("前置通知");
    return proxy.invokeSuper(obj, args);
});
UserService proxy = (UserService) enhancer.create();
```

**原理延伸**：
- JDK 代理：反射调用，性能优化好
- CGLIB：ASM 字节码技术，生成子类
- Spring Boot 2.x 默认使用 CGLIB（proxyTargetClass=true）

---

### 面试题 6：AOP 的底层原理是什么？

**标准应答**：

AOP（面向切面编程）的底层原理是**动态代理 + 拦截器链**。

**核心流程**：
```
1. 扫描@Aspect 切面类
2. 解析@Pointcut 切点表达式
3. 匹配目标类的方法
4. 创建代理对象（JDK/CGLIB）
5. 方法调用时 → 执行拦截器链
   - @Before 通知
   - @Around 前置处理
   - 目标方法
   - @Around 后置处理
   - @After 通知
   - @AfterReturning/@AfterThrowing
```

**拦截器链执行**（责任链模式）：
```java
public Object proceed(JoinPoint joinPoint) {
    if (currentInterceptorIndex == interceptors.size()) {
        return method.invoke(target, args);  // 执行目标方法
    }
    
    Interceptor interceptor = interceptors.get(currentInterceptorIndex++);
    return interceptor.invoke(this);  // 执行下一个拦截器
}
```

**通知执行顺序**：
```
Around (前置) → Before → 目标方法 → Around (后置) → After → AfterReturning
```

**原理延伸**：
- 切点表达式解析：AspectJ 框架
- 织入时机：Bean 初始化后（BeanPostProcessor）
- 性能影响：方法调用增加代理开销

---

### 面试题 7：AOP 的应用场景有哪些？

**标准应答**：

AOP 适用于**横切关注点**（多个类都需要但业务无关的功能）：

1. **日志记录** - 记录方法执行时间、参数、返回值
2. **事务管理** - @Transactional 声明式事务
3. **权限校验** - 方法调用前检查权限
4. **性能监控** - 统计方法执行耗时
5. **异常处理** - 统一异常捕获和处理
6. **缓存处理** - 方法结果缓存

**代码示例**（日志切面）：
```java
@Aspect
@Component
public class LogAspect {
    
    @Around("@annotation(org.slf4j.Logger)")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        
        Object result = pjp.proceed();  // 执行目标方法
        
        long duration = System.currentTimeMillis() - start;
        System.out.println(pjp.getSignature() + " 执行时间：" + duration + "ms");
        
        return result;
    }
}
```

---

## 三、自动配置模块

### 面试题 8：Spring Boot 自动配置原理？

**标准应答**：

Spring Boot 自动配置的核心是**约定优于配置**。

**实现原理**：
```
1. @SpringBootApplication 包含@EnableAutoConfiguration
2. @EnableAutoConfiguration 导入 AutoConfigurationImportSelector
3. 读取 META-INF/spring.factories 文件
4. 加载所有自动配置类（xxxAutoConfiguration）
5. 根据@Conditional 条件注解判断是否生效
   - @ConditionalOnClass：类路径存在某个类
   - @ConditionalOnProperty：配置文件存在某个属性
   - @ConditionalOnMissingBean：容器中不存在某个 Bean
6. 符合条件的配置类 → 注册 Bean 到容器
```

**代码示例**：
```java
// spring.factories 文件
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.bootcloud.boot.autoconfigure.WebServerAutoConfiguration

// 自动配置类
@Configuration
@ConditionalOnClass(NettyWebServer.class)  // 类存在时生效
public class WebServerAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(prefix = "server", name = "port", havingValue = "8080")
    public WebServer webServer() {
        return new NettyWebServer(8080);
    }
}
```

**原理延伸**：
- 自动配置类按需加载（懒加载）
- 可通过@EnableAutoConfiguration(exclude=...)排除
- 自定义 Starter 就是编写自动配置类

---

### 面试题 9：如何自定义一个 Starter？

**标准应答**：

自定义 Starter 的步骤：

1. **创建 Maven 项目**（命名规范：xxx-spring-boot-starter）
2. **编写自动配置类**
3. **编写 spring.factories 文件**
4. **发布到 Maven 仓库**

**示例结构**：
```
my-spring-boot-starter/
├── src/main/java/
│   └── MyServiceAutoConfiguration.java
└── src/main/resources/
    └── META-INF/spring.factories
```

**配置类示例**：
```java
@Configuration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyProperties.class)
public class MyServiceAutoConfiguration {
    
    @Autowired
    private MyProperties properties;
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService() {
        return new MyService(properties.getUrl());
    }
}
```

**spring.factories**：
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.MyServiceAutoConfiguration
```

**使用方式**：
```xml
<!-- pom.xml 引入依赖 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 四、服务注册与发现模块

### 面试题 10：服务注册发现的原理？

**标准应答**：

服务注册发现是微服务架构的**基础设施**，解决服务间通信问题。

**核心概念**：
- **服务提供者** - 提供服务实例
- **服务消费者** - 调用服务
- **注册中心** - 存储服务信息（名称、IP、端口）

**注册流程**：
```
1. 服务启动 → 读取服务名、IP、端口
2. 向注册中心发送注册请求
3. 注册中心存储服务实例（内存/数据库）
4. 定期发送心跳（保持在线状态）
```

**发现流程**：
```
1. 消费者启动 → 从注册中心拉取服务列表
2. 本地缓存服务实例
3. 调用时选择实例（负载均衡）
4. 定期刷新服务列表
```

**本项目实现**：
```java
// 服务注册
@ServiceRegistry
public class InMemoryServiceRegistry {
    private Map<String, List<ServiceInstance>> registry;
    
    public void register(String serviceName, ServiceInstance instance) {
        registry.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(instance);
    }
}

// 服务发现
@ServiceDiscovery
public class DefaultServiceDiscovery {
    public List<ServiceInstance> getInstances(String serviceName) {
        return registry.get(serviceName);
    }
}
```

**原理延伸**：
- 注册中心选型：Eureka（AP）、Nacos（CP/AP）、ZooKeeper（CP）
- 心跳机制：客户端主动上报 vs 服务端被动检测
- 健康检查：剔除不可用实例

---

### 面试题 11：CAP 理论在注册中心的应用？

**标准应答**：

CAP 理论：分布式系统无法同时满足 Consistency（一致性）、Availability（可用性）、Partition tolerance（分区容错性）。

**注册中心的 CAP 选择**：

| 注册中心 | CAP 选择 | 特点 |
|---------|---------|------|
| **Eureka** | AP | 保证可用性，数据可能不一致 |
| **Nacos** | CP/AP 可切换 | 灵活选择 |
| **ZooKeeper** | CP | 保证一致性，可能不可用 |
| **Consul** | CP | 强一致性 |

**AP 模式**（Eureka）：
- 节点对等，无主节点
- 注册信息可能短暂不一致
- 即使部分节点宕机，仍可注册发现

**CP 模式**（ZooKeeper）：
- Leader-Follower 架构
- 数据强一致
- Leader 选举期间不可用

**面试加分项**：
- 生产环境推荐：Nacos（灵活切换）
- 互联网场景：AP 优先（高可用）
- 金融场景：CP 优先（数据一致）

---

## 五、远程调用模块

### 面试题 12：Feign 的声明式调用原理？

**标准应答**：

Feign 的核心思想是**接口即服务**，通过动态代理实现声明式调用。

**实现原理**：
```
1. 扫描@FeignClient 注解的接口
2. 为接口创建动态代理（JDK/CGLIB）
3. 解析接口方法的@RequestMapping 等注解
4. 构建 HTTP 请求（URL、方法、参数、头）
5. 发送 HTTP 请求（HttpClient/OkHttp）
6. 解析响应 → 返回结果
```

**代码示例**：
```java
// 定义 Feign 客户端
@FeignClient(name = "user-service", url = "http://localhost:8081")
public interface UserClient {
    
    @GetMapping("/api/user/{id}")
    User getUserById(@PathVariable("id") Long id);
    
    @PostMapping("/api/user")
    User createUser(@RequestBody User user);
}

// 使用（无需关心 HTTP 细节）
@Autowired
private UserClient userClient;

public void business() {
    User user = userClient.getUserById(1L);  // 像调用本地方法一样
}
```

**代理实现**：
```java
// FeignInvocationHandler.invoke()
public Object invoke(Object proxy, Method method, Object[] args) {
    // 1. 解析方法注解
    RequestTemplate template = parseAnnotations(method);
    
    // 2. 负载均衡选择实例
    ServiceInstance instance = loadBalancer.choose(serviceName);
    
    // 3. 构建请求
    Request request = template.request(instance);
    
    // 4. 发送 HTTP 请求
    Response response = client.execute(request);
    
    // 5. 解码响应
    return decoder.decode(response, method.getReturnType());
}
```

**原理延伸**：
- 性能优化：连接池（HttpClient/OkHttp）
- 序列化：JSON（Jackson）、Protobuf
- 超时控制：connectTimeout、readTimeout

---

### 面试题 13：HTTP 和 RPC 的区别？

**标准应答**：

| 对比项 | HTTP（REST） | RPC |
|--------|-------------|-----|
| **协议** | HTTP/HTTPS | 自定义 TCP 协议 |
| **序列化** | JSON/XML | Protobuf/Thrift |
| **性能** | 较慢（文本协议） | 快（二进制协议） |
| **可读性** | 好（人类可读） | 差（二进制） |
| **跨语言** | 好（标准协议） | 需框架支持 |
| **使用场景** | 对外 API、Web 服务 | 内部服务调用 |

**性能对比**：
```
HTTP + JSON:     1000 QPS, 50ms 延迟
RPC + Protobuf:  5000 QPS, 10ms 延迟
```

**Protobuf 优势**：
- 二进制编码，体积小（1/10 of JSON）
- 编解码快（3-5 倍性能提升）
- 强类型，向后兼容

**本项目实现**：
```java
// JSON 序列化
public class JsonEncoder implements Encoder {
    public byte[] encode(Object obj) {
        return Jackson.toJsonBytes(obj);
    }
}

// Protobuf 序列化（可选）
public class ProtobufEncoder implements Encoder {
    public byte[] encode(Object obj) {
        return ProtobufUtil.toByteArray(obj);
    }
}
```

---

## 六、负载均衡模块

### 面试题 14：负载均衡算法有哪些？

**标准应答**：

负载均衡分为**服务端负载均衡**和**客户端负载均衡**。

**常见算法**：

1. **轮询（Round Robin）**
   - 按顺序轮流分配
   - 适用：服务器性能相近
   
2. **随机（Random）**
   - 随机选择实例
   - 适用：实例数多，概率均匀

3. **权重轮询（Weighted Round Robin）**
   - 按权重分配（性能强的权重高）
   - 适用：服务器性能差异大

4. **最少活跃数（Least Active）**
   - 选择活跃请求数最少的实例
   - 适用：请求耗时差异大

5. **一致性哈希（Consistent Hash）**
   - 相同参数请求到同一实例
   - 适用：缓存场景

**代码示例**（轮询）：
```java
public class RoundRobinLoadBalancer implements LoadBalancer {
    private AtomicInteger position = new AtomicInteger(0);
    
    public ServiceInstance choose(List<ServiceInstance> instances) {
        int index = Math.abs(position.getAndIncrement() % instances.size());
        return instances.get(index);
    }
}
```

**代码示例**（权重轮询）：
```java
public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    public ServiceInstance choose(List<ServiceInstance> instances) {
        // 按权重构建权重数组
        List<ServiceInstance> weightedList = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            for (int i = 0; i < instance.getWeight(); i++) {
                weightedList.add(instance);
            }
        }
        // 随机选择
        int index = ThreadLocalRandom.current().nextInt(weightedList.size());
        return weightedList.get(index);
    }
}
```

---

### 面试题 15：客户端负载均衡 vs 服务端负载均衡？

**标准应答**：

**服务端负载均衡**（Nginx、F5）：
```
客户端 → 负载均衡器 → 多个服务实例
         （集中式）
```

**客户端负载均衡**（Ribbon、本项目）：
```
客户端（内置负载均衡器） → 多个服务实例
     （分布式）
```

**对比**：

| 对比项 | 服务端负载均衡 | 客户端负载均衡 |
|--------|--------------|--------------|
| **架构** | 集中式 | 分布式 |
| **性能** | 单点瓶颈 | 无瓶颈 |
| **复杂度** | 简单 | 复杂 |
| **灵活性** | 低 | 高（可定制策略） |
| **使用场景** | 对外服务 | 内部服务调用 |

**Spring Cloud 实现**：
- Ribbon（客户端）
- LoadBalancer（新一代）
- 本项目：[LoadBalancerFactory](file:///d:/Work/Java/Boot&Cloud/mini-spring-cloud-loadbalancer/src/main/java/com/bootcloud/loadbalancer/factory/LoadBalancerFactory.java)

---

## 七、服务熔断与降级模块

### 面试题 16：熔断器的原理是什么？

**标准应答**：

熔断器（Circuit Breaker）是一种**保护机制**，防止服务雪崩。

**三种状态**：
```
CLOSED（闭合） → OPEN（打开） → HALF_OPEN（半开）
      ↑                              ↓
      └──────────────────────────────┘
```

**状态转换**：
1. **CLOSED** - 正常状态
   - 正常处理请求
   - 统计失败率
   - 失败率超过阈值 → OPEN

2. **OPEN** - 熔断状态
   - 直接快速失败
   - 不调用远程服务
   - 超时后 → HALF_OPEN

3. **HALF_OPEN** - 探测状态
   - 允许一次请求
   - 成功 → CLOSED
   - 失败 → OPEN

**代码示例**：
```java
public class DefaultCircuitBreaker {
    private CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private int failureCount = 0;
    private long lastFailureTime = 0;
    
    public <T> T execute(Callable<T> callable) {
        if (state == CircuitBreakerState.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > timeout) {
                state = CircuitBreakerState.HALF_OPEN;
            } else {
                throw new CircuitBreakerOpenException("熔断器打开");
            }
        }
        
        try {
            T result = callable.call();
            if (state == CircuitBreakerState.HALF_OPEN) {
                state = CircuitBreakerState.CLOSED;  // 成功，恢复
            }
            return result;
        } catch (Exception e) {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            if (failureCount >= failureThreshold) {
                state = CircuitBreakerState.OPEN;  // 失败，熔断
            }
            throw e;
        }
    }
}
```

**原理延伸**：
- Hystrix（已停更）、Resilience4j（推荐）
- 降级策略（Fallback）：返回默认值、缓存数据
- 避免服务雪崩的关键技术

---

### 面试题 17：服务降级的应用场景？

**标准应答**：

服务降级（Fallback）是熔断后的**兜底方案**。

**应用场景**：
1. **返回默认值**
   - 商品详情不可用 → 返回空对象
   - 用户信息不可用 → 返回匿名用户

2. **返回缓存数据**
   - 数据库不可用 → 返回本地缓存
   - 接口超时 → 返回历史数据

3. **限流降级**
   - 秒杀场景 → 返回"排队中"
   - 流量高峰 → 返回"系统繁忙"

4. **功能降级**
   - 推荐系统不可用 → 返回热门商品
   - 积分系统不可用 → 暂不计算积分

**代码示例**：
```java
@FeignClient(name = "product-service", fallback = ProductFallback.class)
public interface ProductClient {
    @GetMapping("/api/product/{id}")
    Product getProduct(@PathVariable Long id);
}

@Component
public class ProductFallback implements ProductClient {
    @Override
    public Product getProduct(Long id) {
        // 降级逻辑：返回空产品
        Product product = new Product();
        product.setId(id);
        product.setName("商品已下架");
        return product;
    }
}
```

---

## 八、JVM 调优模块

### 面试题 18：JVM 内存模型是怎样的？

**标准应答**：

JVM 内存模型（JMM）分为**线程私有**和**线程共享**两部分。

**内存结构**：
```
┌─────────────────────────────────────┐
│         堆（Heap） - 线程共享        │
│  ┌─────────┐  ┌─────────────────┐  │
│  │新生代   │  │ 老年代           │  │
│  │ Eden    │  │                  │  │
│  │ S0      │  │                  │  │
│  │ S1      │  │                  │  │
│  └─────────┘  └─────────────────┘  │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│      栈（Stack） - 线程私有          │
│  - 局部变量表                        │
│  - 操作数栈                          │
│  - 帧数据                            │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│    元空间（Metaspace） - 线程共享    │
│  - 类信息                            │
│  - 常量池                            │
│  - 方法信息                          │
└─────────────────────────────────────┘
```

**各区域作用**：
- **堆**：存储对象实例（GC 主要区域）
- **栈**：方法调用栈帧（局部变量、方法调用）
- **元空间**：类元数据（JDK 8 替代永久代）
- **程序计数器**：当前执行的字节码行号
- **本地方法栈**：Native 方法

**常见 OOM**：
- `OutOfMemoryError: Java heap space` - 堆内存不足
- `OutOfMemoryError: Metaspace` - 元空间不足
- `StackOverflowError` - 栈溢出（递归过深）

**调优参数**：
```bash
# 堆大小
-Xms2g -Xmx2g                    # 初始/最大堆
-Xmn512m                         # 新生代大小

# 元空间
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m

# 栈大小
-Xss256k                         # 线程栈大小
```

---

### 面试题 19：GC 算法有哪些？

**标准应答**：

GC（垃圾回收）算法基于**分代收集理论**。

**分代理论**：
- 新生代：对象朝生夕死（复制算法）
- 老年代：对象长期存活（标记整理算法）

**常见算法**：

1. **标记 - 清除（Mark-Sweep）**
   - 标记存活对象 → 清除未标记对象
   - 缺点：内存碎片

2. **复制（Copying）**
   - 存活对象复制到另一块内存
   - 优点：无碎片
   - 缺点：内存利用率低（新生代使用）

3. **标记 - 整理（Mark-Compact）**
   - 标记存活对象 → 向一端压缩
   - 优点：无碎片
   - 缺点：移动对象开销大（老年代使用）

**GC 收集器**：

| 收集器 | 代别 | 算法 | 特点 |
|--------|------|------|------|
| **Serial** | 新生代 | 复制 | 单线程，简单 |
| **ParNew** | 新生代 | 复制 | 多线程 |
| **CMS** | 老年代 | 标记清除 | 低停顿 |
| **G1** | 全堆 | 分区 | 可预测停顿 |
| **ZGC** | 全堆 | 染色指针 | 停顿<10ms |

**G1GC 调优**：
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200      # 最大停顿时间
-XX:G1HeapRegionSize=16m      # 区域大小
-XX:InitiatingHeapOccupancyPercent=45  # 触发 GC 阈值
```

---

### 面试题 20：如何排查 OOM 问题？

**标准应答**：

OOM 排查步骤：

**1. 保留现场**
```bash
# 添加 JVM 参数，OOM 时自动 dump
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof
```

**2. 查看 GC 日志**
```bash
# 开启 GC 日志
-Xloggc:/tmp/gc.log
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps

# 分析 GC 频率、停顿时间
```

**3. 使用工具分析**
```bash
# jstat - 查看 GC 统计
jstat -gcutil <pid> 1000

# jmap - 导出堆 dump
jmap -dump:format=b,file=heap.hprof <pid>

# jstack - 线程 dump
jstack <pid> > thread.txt

# MAT - 图形化分析堆 dump
```

**4. 常见原因**
- 内存泄漏（静态集合、未关闭资源）
- 内存不足（堆太小、对象过多）
- 大对象（大数组、大字符串）

**5. 解决方案**
- 增加堆内存（-Xmx）
- 修复内存泄漏
- 优化数据结构
- 分页/分批处理

**本项目实现**：
- [MemoryLeakSimulator](file:///d:/Work/Java/Boot&Cloud/jvm-optimizer/src/main/java/com/bootcloud/jvm/MemoryLeakSimulator.java) - 内存泄漏模拟
- [JVMProfiler](file:///d:/Work/Java/Boot&Cloud/jvm-optimizer/src/main/java/com/bootcloud/jvm/JVMProfiler.java) - JVM 性能分析
- [GCTuner](file:///d:/Work/Java/Boot&Cloud/jvm-optimizer/src/main/java/com/bootcloud/jvm/GCTuner.java) - GC 调优建议

---

## 九、并发编程模块

### 面试题 21：线程池的原理是什么？

**标准应答**：

线程池的核心思想是**线程复用**，避免频繁创建销毁线程。

**ThreadPoolExecutor 参数**：
```java
new ThreadPoolExecutor(
    int corePoolSize,      // 核心线程数
    int maximumPoolSize,   // 最大线程数
    long keepAliveTime,    // 空闲线程存活时间
    TimeUnit unit,         // 时间单位
    BlockingQueue<Runnable> workQueue,  // 任务队列
    ThreadFactory threadFactory,        // 线程工厂
    RejectedExecutionHandler handler    // 拒绝策略
)
```

**执行流程**：
```
1. 提交任务 → 核心线程是否已满？
   - 否 → 创建新线程执行
   - 是 → 进入下一步

2. 任务队列是否已满？
   - 否 → 放入队列等待
   - 是 → 进入下一步

3. 线程数是否达到最大值？
   - 否 → 创建非核心线程执行
   - 是 → 进入下一步

4. 执行拒绝策略
   - AbortPolicy：抛出异常（默认）
   - CallerRuns：调用者线程执行
   - Discard：直接丢弃
   - DiscardOldest：丢弃最老任务
```

**线程池配置建议**：
```java
// CPU 密集型（计算密集）
int coreSize = CPU 核数 + 1;
int maxSize = CPU 核数 + 1;

// IO 密集型（网络、数据库）
int coreSize = CPU 核数 * 2;
int maxSize = CPU 核数 * 4;

// 混合型
int coreSize = CPU 核数;
int maxSize = CPU 核数 * 2;
```

**本项目实现**：
- [SmartThreadPool](file:///d:/Work/Java/Boot&Cloud/concurrent-optimizer/src/main/java/com/bootcloud/concurrent/SmartThreadPool.java) - 智能线程池
- 支持动态调参、统计监控

---

### 面试题 22：synchronized 锁升级过程？

**标准应答**：

synchronized 在 JDK 6 后进行了优化，引入了**锁升级**机制。

**锁的四种状态**：
```
无锁 → 偏向锁 → 轻量级锁 → 重量级锁
```

**升级过程**：

1. **无锁**
   - 没有竞争，对象头记录线程 ID

2. **偏向锁**（JDK 6 引入）
   - 第一个获取锁的线程"偏向"于该线程
   - 无需 CAS 操作
   - 适用：单线程访问

3. **轻量级锁**（自旋锁）
   - 出现竞争，偏向锁升级为轻量级锁
   - 线程自旋等待（不阻塞）
   - 适用：短时间持有锁

4. **重量级锁**
   - 自旋失败，升级为重量级锁
   - 线程阻塞，操作系统介入
   - 适用：长时间持有锁

**对象头结构**（64 位 JVM）：
```
┌──────────────────────┐
│ Mark Word (64 位)     │  ← 存储锁信息
├──────────────────────┤
│ Klass Pointer (64 位) │  ← 类指针
├──────────────────────┤
│ 数组长度（可选）      │
└──────────────────────┘
```

**性能对比**：
```
无锁/偏向锁：   1000 万 QPS
轻量级锁：     500 万 QPS
重量级锁：     100 万 QPS
```

---

### 面试题 23：CAS 的原理和缺点？

**标准应答**：

CAS（Compare And Swap）是**无锁编程**的基础。

**原理**：
```
CAS(V, E, N)  // V=内存值，E=期望值，N=新值

1. 比较内存值 V 和期望值 E
2. 如果相等 → 更新为新值 N
3. 如果不等 → 重试（自旋）
```

**硬件支持**：
- CPU 的 CMPXCHG 指令
- 保证原子性

**代码示例**：
```java
// AtomicInteger 实现
public final int getAndIncrement() {
    for (;;) {
        int current = get();
        int next = current + 1;
        if (compareAndSet(current, next)) {  // CAS 操作
            return current;
        }
        // 失败则重试
    }
}
```

**优点**：
- 无锁，性能高
- 不会死锁
- 线程安全

**缺点**：
1. **ABA 问题**
   - 值从 A→B→A，CAS 认为没变化
   - 解决：版本号（AtomicStampedReference）

2. **自旋开销**
   - 长时间自旋浪费 CPU
   - 解决：限制自旋次数

3. **只能保证单个变量**
   - 多个变量需用锁
   - 解决：LongAdder（分段 CAS）

**应用场景**：
- 原子类（AtomicInteger、AtomicReference）
- AQS（AbstractQueuedSynchronizer）
- 无锁队列（ConcurrentLinkedQueue）

---

### 面试题 24：如何避免死锁？

**标准应答**：

死锁的四个必要条件：
1. 互斥条件
2. 请求与保持
3. 不剥夺条件
4. 循环等待

**死锁示例**：
```java
// 死锁场景
Thread1: synchronized(A) { synchronized(B) { } }
Thread2: synchronized(B) { synchronized(A) { } }
```

**避免方法**：

1. **固定锁顺序**
   ```java
   // 总是先获取 A，再获取 B
   synchronized(A) {
       synchronized(B) { }
   }
   ```

2. **使用定时锁**
   ```java
   if (lock.tryLock(1, TimeUnit.SECONDS)) {
       try {
           // 业务逻辑
       } finally {
           lock.unlock();
       }
   }
   ```

3. **使用并发工具**
   - ReentrantLock（可中断）
   - Semaphore（信号量）
   - ConcurrentHashMap（无锁并发）

4. **减少锁粒度**
   - 缩小同步范围
   - 使用读写锁

**死锁检测**：
```bash
# jstack 查看线程 dump
jstack <pid>

# 发现死锁
Found one Java-level deadlock:
"Thread-1": waiting to lock monitor A
"Thread-2": waiting to lock monitor B
```

**本项目实现**：
- [DeadlockDetector](file:///d:/Work/Java/Boot&Cloud/concurrent-optimizer/src/main/java/com/bootcloud/concurrent/DeadlockDetector.java) - 死锁检测工具
- [LockComparator](file:///d:/Work/Java/Boot&Cloud/concurrent-optimizer/src/main/java/com/bootcloud/concurrent/LockComparator.java) - 锁性能对比

---

## 十、综合设计题

### 面试题 25：设计一个高并发秒杀系统？

**标准应答**：

秒杀系统的核心挑战：**瞬时高并发**。

**架构设计**：
```
用户 → CDN（静态资源） → Nginx（限流） → 网关（鉴权）
                                    ↓
                            Redis（库存预热）
                                    ↓
                            消息队列（削峰）
                                    ↓
                            数据库（最终扣减）
```

**关键技术**：

1. **前端优化**
   - 按钮防重复点击
   - 倒计时同步
   - 静态资源 CDN

2. **限流降级**
   - Nginx 限流（令牌桶）
   - 网关层限流（Guava RateLimiter）
   - 服务降级（返回排队提示）

3. **缓存预热**
   - 秒杀前加载库存到 Redis
   - Redis 原子扣减（DECR）

4. **异步处理**
   - 扣减成功后发送消息
   - 异步创建订单

5. **防刷机制**
   - 验证码
   - IP 限流
   - 用户限流

**代码示例**（Redis 扣减库存）：
```java
public boolean seckill(Long productId) {
    String key = "seckill:stock:" + productId;
    
    // Redis 原子扣减
    Long stock = redisTemplate.opsForValue().decrement(key);
    
    if (stock < 0) {
        redisTemplate.opsForValue().increment(key);  // 回滚
        return false;  // 库存不足
    }
    
    // 发送消息，异步创建订单
    kafkaTemplate.send("seckill-topic", productId);
    
    return true;
}
```

---

### 面试题 26：如何设计一个分布式 ID 生成器？

**标准应答**：

分布式 ID 要求：**全局唯一、趋势递增、高可用**。

**常见方案**：

1. **UUID**
   - 优点：本地生成，性能好
   - 缺点：无序，不适合索引

2. **数据库自增 ID**
   - 优点：简单，有序
   - 缺点：单点，性能差

3. **Redis 自增**
   - 优点：性能好，有序
   - 缺点：依赖 Redis

4. **Snowflake（雪花算法）** ⭐推荐
   - 优点：性能好，有序，不依赖外部
   - 缺点：时钟回拨问题

**Snowflake 结构**（64 位）：
```
┌───┬─────────────┬──────────┬──────────────┐
│ 0 │ 时间戳 (41 位) │ 机器 ID  │   序列号     │
│   │             │  (10 位)  │   (12 位)    │
└───┴─────────────┴──────────┴──────────────┘
```

**代码示例**：
```java
public class SnowflakeIdGenerator {
    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        // 时钟回拨检查
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨");
        }
        
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095;  // 12 位序列号
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - EPOCH) << 22)  // 时间戳
             | (workerId << 12)             // 机器 ID
             | sequence;                    // 序列号
    }
}
```

---

## 总结

本文档整理了 Boot&Cloud 框架涉及的**26 道高频面试题**，涵盖：

- ✅ IOC 容器（4 题）
- ✅ AOP（3 题）
- ✅ 自动配置（2 题）
- ✅ 服务注册发现（2 题）
- ✅ 远程调用（2 题）
- ✅ 负载均衡（2 题）
- ✅ 熔断降级（2 题）
- ✅ JVM 调优（3 题）
- ✅ 并发编程（4 题）
- ✅ 综合设计（2 题）

**使用建议**：
1. 先理解原理，再背诵答案
2. 结合代码实现，加深理解
3. 举一反三，扩展思考
4. 实战演练，面试模拟

**祝面试顺利，Offer 多多！** 🚀

---

**文档版本**：v1.0  
**最后更新**：2026-03-29  
**作者**：Boot&Cloud 开发团队
