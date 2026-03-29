# Boot&Cloud 架构设计文档

## 一、项目概述

### 1.1 项目定位
Boot&Cloud是一个从零手写的极简Java微服务框架，深度复刻Spring Boot + Spring Cloud核心能力，集成JVM调优、多线程与锁优化模块，聚焦Java后端面试高频考点。

### 1.2 技术栈
- **JDK版本**: JDK 17+
- **网络通信**: Netty
- **序列化**: Jackson, Protobuf
- **测试框架**: JUnit 5
- **日志框架**: SLF4J + Logback
- **构建工具**: Maven

### 1.3 核心目标
- 代码落地：从零实现核心功能，手写所有关键逻辑
- 原理吃透：深度理解底层实现机制（反射、动态代理、类加载等）
- 面试应答：每个模块绑定面试考点，提供标准问答

---

## 二、架构分层设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        示例应用层                               │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│   │user-service │  │order-service│  │goods-service│            │
│   └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────────┐
│                      API网关层                                   │
│                    mini-spring-gateway                          │
│            路由转发 | 请求过滤 | 跨域处理 | 限流                  │
└─────────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────────┐
│                     微服务组件层                                 │
│  ┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │   Registry   │ │  Feign   │ │LoadBalancer││CircuitBreaker│   │
│  │   注册中心   │ │ 远程调用 │ │ 负载均衡  │ │  熔断降级   │   │
│  └──────────────┘ └──────────┘ └──────────┘ └──────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────────┐
│                    核心容器层                                    │
│  ┌──────────────────────┐  ┌──────────────────────┐          │
│  │   mini-spring-core   │  │  mini-spring-boot    │          │
│  │   IOC | AOP容器      │  │  自动配置|嵌入式容器  │          │
│  └──────────────────────┘  └──────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────────┐
│                    性能优化层                                    │
│  ┌──────────────────────┐  ┌──────────────────────┐          │
│  │    jvm-optimizer     │  │ concurrent-optimizer │          │
│  │   JVM调优|GC监控     │  │  多线程|锁优化       │          │
│  └──────────────────────┘  └──────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块职责划分

| 模块名称 | 职责描述 | 面试考点 |
|---------|---------|---------|
| mini-spring-core | IOC容器、AOP实现 | 控制反转、依赖注入、Bean生命周期、循环依赖、动态代理、AOP原理 |
| mini-spring-boot | 自动配置、嵌入式容器 | 自动配置原理、Starter机制、嵌入式服务器、请求处理流程 |
| mini-spring-cloud-registry | 服务注册与发现 | 服务注册发现原理、心跳机制、健康检查、CAP理论 |
| mini-spring-cloud-feign | 远程服务调用 | 声明式RPC、序列化协议、HTTP vs RPC、负载均衡 |
| mini-spring-cloud-loadbalancer | 客户端负载均衡 | 负载均衡算法、加权策略、最少活跃数 |
| mini-spring-cloud-circuitbreaker | 服务熔断与降级 | 熔断器状态机、降级策略、服务雪崩防护 |
| mini-spring-gateway | API网关 | 网关设计模式、路由转发、请求过滤、限流 |
| jvm-optimizer | JVM调优与监控 | JVM内存模型、GC算法、G1/ZGC调优、OOM排查 |
| concurrent-optimizer | 多线程与锁优化 | 线程池原理、锁优化、CAS、并发工具、死锁排查 |
| demo-app | 示例微服务应用 | 整体架构理解、端到端流程演示 |

---

## 三、模块依赖关系图

```
                           ┌──────────────────┐
                           │     demo-app     │
                           └─────────┬────────┘
                                     │
                ┌────────────────────┼────────────────────┐
                │                    │                    │
        ┌───────▼────────┐  ┌──────▼──────┐  ┌────────▼────────┐
        │mini-spring-gateway│ │jvm-optimizer│ │concurrent-optimizer│
        └───────┬────────┘  └─────────────┘  └─────────────────┘
                │
        ┌───────┼────────┐
        │       │        │
┌───────▼───┐ ┌▼────────▼──┐ ┌──────────────────┐
│Registry   │ │Feign+LoadBalancer│ │CircuitBreaker   │
└───────┬───┘ └────────────┘ └──────────────────┘
        │
        │
┌───────▼──────────────────────┐
│ mini-spring-boot              │
│ (依赖mini-spring-core)        │
└───────┬──────────────────────┘
        │
┌───────▼──────────────────────┐
│ mini-spring-core              │
│ (基础依赖，不依赖其他模块)     │
└───────────────────────────────┘
```

---

## 四、核心流程图

### 4.1 Bean生命周期流程

```
┌─────────────────────────────────────────────────────────────┐
│                   Bean生命周期流程                           │
└─────────────────────────────────────────────────────────────┘

开始
  ↓
1. 扫描类路径（识别@Component/@Service/@Repository/@Controller）
  ↓
2. 解析BeanDefinition（封装Bean元数据）
  ↓
3. 实例化Bean（反射创建对象）
  ↓
4. 属性填充（@Autowired字段注入/构造器注入/Setter注入）
  ↓
5. 执行@PostConstruct方法（初始化回调）
  ↓
6. 应用BeanPostProcessor后置处理器（AOP代理在此阶段生成）
  ↓
7. Bean就绪（可被使用）
  ↓
8. 执行@PreDestroy方法（销毁回调）
  ↓
9. Bean销毁（GC回收）
  ↓
结束

面试考点：
- 三级缓存如何解决循环依赖？
- BeanPostProcessor的作用？
- AOP代理在哪个阶段生成？
```

### 4.2 AOP代理生成流程

```
┌─────────────────────────────────────────────────────────────┐
│                   AOP代理生成流程                            │
└─────────────────────────────────────────────────────────────┘

开始
  ↓
1. 扫描@Aspect注解的切面类
  ↓
2. 解析切面定义（@Pointcut/@Before/@After/@Around）
  ↓
3. 构建Advisor链（将切面通知与目标方法匹配）
  ↓
4. 判断目标类是否实现接口
  ↓
   ┌─────────────┬─────────────┐
   │             │             │
   ↓             ↓             ↓
实现接口      未实现接口    强制使用CGLIB
   │             │             │
   ↓             ↓             ↓
JDK动态代理   CGLIB代理     CGLIB代理
   │             │             │
   └─────────────┴─────────────┘
                 ↓
5. 生成代理对象
  ↓
6. 方法调用时触发拦截器链
  ↓
结束

面试考点：
- JDK动态代理 vs CGLIB代理的区别？
- 为什么Spring默认使用JDK代理？
- @Around通知的执行顺序？
```

### 4.3 服务注册与发现流程

```
┌─────────────────────────────────────────────────────────────┐
│                 服务注册与发现流程                          │
└─────────────────────────────────────────────────────────────┘

服务提供方启动                    服务消费方启动
      ↓                                  ↓
1. 解析@EnableServiceProvider      1. 解析@EnableServiceConsumer
      ↓                                  ↓
2. 读取服务配置（IP、端口、服务名） 2. 从注册中心拉取服务列表
      ↓                                  ↓
3. 向注册中心发送注册请求          3. 本地缓存服务实例
      ↓                                  ↓
4. 定时发送心跳（保持连接）        4. 定时刷新服务列表
      ↓                                  ↓
5. 关闭时发送注销请求              5. 过滤不健康实例
                                         ↓
                              6. 结合负载均衡选择实例
                                         ↓
                              7. 发起远程调用

面试考点：
- 服务注册中心如何保证一致性？
- 心跳机制如何设计？
- 如何处理服务下线？
```

### 4.4 熔断器状态转换流程

```
┌─────────────────────────────────────────────────────────────┐
│                 熔断器状态机流程                              │
└─────────────────────────────────────────────────────────────┘

              ┌──────────┐
              │  CLOSED  │ ◄──┐
              │ (闭合)   │    │
              └────┬─────┘    │
                   │          │ 失败率<阈值
         失败率≥阈值 │          │ 或 成功恢复
                   │          │
                   ▼          │
              ┌──────────┐    │
              │   OPEN   │ ───┘
              │ (打开)   │
              └────┬─────┘
                   │
         半开超时时间 │
                   │
                   ▼
              ┌──────────┐
              │ HALF_OPEN│
              │  (半开)  │
              └────┬─────┘
                   │
      ┌────────────┼────────────┐
      │            │            │
      ▼            ▼            ▼
   调用成功      调用失败
      │            │
      │            │
      ▼            ▼
    CLOSED       OPEN
   (恢复闭合)   (重新打开)

面试考点：
- 熔断器三种状态的切换条件？
- 为什么需要半开状态？
- 熔断与降级的区别？
```

### 4.5 完整请求处理流程

```
┌─────────────────────────────────────────────────────────────┐
│                 完整请求处理流程                              │
└─────────────────────────────────────────────────────────────┘

客户端请求
      ↓
┌─────────────────────────────────────┐
│  API网关                            │
│  1. 路由匹配                        │
│  2. 请求过滤（鉴权、限流）          │
│  3. 跨域处理                        │
└───────────────┬─────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  目标微服务                        │
│  1. 嵌入式容器接收请求              │
│  2. 路径映射到Controller方法        │
│  3. 参数解析                        │
│  4. AOP拦截器链执行                │
│  5. 业务逻辑处理                    │
└───────────────┬─────────────────────┘
                │
        ┌───────┴───────┐
        │               │
        ▼               ▼
    本地处理      需要远程调用
        │               │
        │               ▼
        │    ┌────────────────────────┐
        │    │  服务发现              │
        │    │  1. 查询服务列表        │
        │    │  2. 负载均衡选择实例    │
        │    └───────────┬────────────┘
        │                │
        │                ▼
        │    ┌────────────────────────┐
        │    │  熔断器检查            │
        │    │  熔断打开→降级         │
        │    │  熔断关闭→发起调用     │
        │    └───────────┬────────────┘
        │                │
        │                ▼
        │    ┌────────────────────────┐
        │    │  Feign远程调用         │
        │    │  1. 序列化请求参数      │
        │    │  2. HTTP/RPC通信       │
        │    │  3. 反序列化响应        │
        │    └───────────┬────────────┘
        │                │
        └───────┬────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  响应处理                          │
│  1. 统一异常处理                    │
│  2. 响应序列化                      │
│  3. 返回客户端                      │
└─────────────────────────────────────┘
```

---

## 五、核心接口设计

### 5.1 IOC容器核心接口

```java
BeanFactory - 基础Bean管理接口
├── getBean(String name) - 根据名称获取Bean
├── getBean(Class<T> type) - 根据类型获取Bean
├── containsBean(String name) - 判断Bean是否存在
└── isSingleton(String name) - 判断是否为单例

ApplicationContext - 高级Bean管理接口（继承BeanFactory）
├── refresh() - 刷新容器（启动流程）
├── getEnvironment() - 获取环境配置
├── publishEvent(ApplicationEvent) - 发布事件
└── registerShutdownHook() - 注册关闭钩子
```

### 5.2 AOP核心接口

```java
Advisor - 切面通知接口
├── getPointcut() - 获取切点表达式
└── getAdvice() - 获取通知逻辑

ProxyFactory - 代理工厂接口
├── createProxy() - 创建代理对象
├── setInterfaces() - 设置接口
└── setTarget() - 设置目标对象

MethodInterceptor - 方法拦截器接口
└── invoke(MethodInvocation) - 拦截方法调用
```

### 5.3 服务注册中心接口

```java
ServiceRegistry - 服务注册接口
├── register(ServiceInstance) - 注册服务
├── deregister(ServiceInstance) - 注销服务
└── heartbeat(ServiceInstance) - 发送心跳

ServiceDiscovery - 服务发现接口
├── getInstances(String serviceName) - 获取服务实例列表
└── subscribe(String serviceName, Listener) - 订阅服务变更
```

### 5.4 负载均衡接口

```java
LoadBalancer - 负载均衡器接口
└── choose(List<ServiceInstance>) - 选择服务实例

LoadBalanceStrategy - 负载均衡策略接口
├── RoundRobinStrategy - 轮询策略
├── RandomStrategy - 随机策略
├── WeightedStrategy - 加权策略
└── LeastActiveStrategy - 最少活跃数策略
```

### 5.5 熔断器接口

```java
CircuitBreaker - 熔断器接口
├── execute(Supplier<T>) - 执行被保护的方法
├── getState() - 获取当前状态
├── reset() - 重置熔断器
└── configure(CircuitBreakerConfig) - 配置参数

CircuitBreakerState - 熔断器状态枚举
├── CLOSED - 闭合（正常）
├── OPEN - 打开（熔断）
└── HALF_OPEN - 半开（尝试恢复）
```

---

## 六、配置文件设计

### 6.1 application.properties 示例

```properties
# 服务器配置
server.port=8080
server.host=0.0.0.0

# 服务注册配置
spring.application.name=user-service
registry.server.host=localhost
registry.server.port=8761
registry.heartbeat.interval=30

# Feign配置
feign.timeout.connect=5000
feign.timeout.read=10000
feign.serialization.type=json

# 负载均衡配置
loadbalancer.strategy=round_robin

# 熔断器配置
circuitbreaker.enabled=true
circuitbreaker.failure.threshold=50
circuitbreaker.timeout=60000
circuitbreaker.half.open.requests=3

# JVM调优配置
jvm.heap.size=512m
jvm.gc.collector=G1
jvm.gc.max.pause=200

# 线程池配置
thread.pool.core.size=8
thread.pool.max.size=16
thread.pool.queue.capacity=100
thread.pool.rejection.policy=caller_runs
```

---

## 七、面试考点总结

### 7.1 IOC容器核心考点

| 考点 | 关键知识点 |
|-----|----------|
| 控制反转 vs 依赖注入 | IoC是思想，DI是实现方式 |
| Bean生命周期 | 实例化→属性填充→初始化→使用→销毁 |
| 循环依赖解决 | 三级缓存：singletonObjects、earlySingletonObjects、singletonFactories |
| Bean作用域 | singleton、prototype、request、session |
| @Autowired原理 | 类型匹配+名称匹配，支持字段、构造器、Setter注入 |

### 7.2 AOP核心考点

| 考点 | 关键知识点 |
|-----|----------|
| AOP核心概念 | 切面、切点、通知、连接点 |
| JDK动态代理 | 基于接口，使用Proxy类和InvocationHandler |
| CGLIB代理 | 基于继承，使用字节码增强 |
| 通知类型 | @Before、@After、@Around、@AfterReturning、@AfterThrowing |
| 代理选择策略 | 有接口用JDK，无接口用CGLIB |

### 7.3 Spring Boot核心考点

| 考点 | 关键知识点 |
|-----|----------|
| 自动配置原理 | @EnableAutoConfiguration + spring.factories + @Conditional |
| Starter机制 | 依赖传递+自动配置 |
| 嵌入式容器 | Tomcat/Jetty/Undertow/Netty |
| 启动流程 | SpringApplication.run() → 创建ApplicationContext → 刷新容器 |

### 7.4 微服务核心考点

| 考点 | 关键知识点 |
|-----|----------|
| 服务注册发现 | CAP理论、AP vs CP、心跳机制 |
| 负载均衡算法 | 轮询、随机、加权、最少连接 |
| 熔断降级 | 三种状态、熔断器模式、降级策略 |
| RPC vs HTTP | 协议差异、性能对比、适用场景 |
| 网关设计模式 | 路由转发、请求过滤、限流 |

### 7.5 JVM调优核心考点

| 考点 | 关键知识点 |
|-----|----------|
| JVM内存模型 | 堆、栈、方法区、程序计数器、本地方法栈 |
| GC算法 | 标记清除、标记整理、复制算法、分代收集 |
| G1收集器 | Region划分、混合GC、停顿时间目标 |
| ZGC收集器 | 读屏障、染色指针、并发整理 |
| OOM排查 | jmap、jstat、jstack、MAT分析 |

### 7.6 并发编程核心考点

| 考点 | 关键知识点 |
|-----|----------|
| 线程池原理 | 核心线程、最大线程、队列、拒绝策略 |
| synchronized锁升级 | 偏向锁→轻量级锁→重量级锁 |
| ReentrantLock | 可重入锁、公平锁、条件变量 |
| CAS原理 | Compare-And-Swap、ABA问题、Unsafe类 |
| 并发工具 | CountDownLatch、CyclicBarrier、Semaphore |
| 死锁排查 | jstack检测、死锁四个条件 |
