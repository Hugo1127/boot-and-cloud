# CLAUDE.md — Boot&Cloud 项目开发规范与约束

> 手写极简 Java 微服务框架，深度复刻 Spring Boot + Spring Cloud 核心能力。
> 本文档为项目永久开发约束，**所有后续开发（含 AI 辅助）必须严格遵守**。

---

## 1. 项目定位与核心目标

- **项目名称**：Boot&Cloud
- **核心定位**：从零手写一个极简但完整的 Java 微服务框架，用于 **面试备战** 与 **原理学习**
- **交付物**：可运行源码 + 架构图 + README + 面试题总结
- **非生产项目**：代码以"原理展示 + 面试演示"为导向，不追求工业级健壮性，但必须保证代码可编译、可运行、逻辑正确

---

## 2. 技术栈（强制约束）

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | **25**（统一版本，不得低于 17） | 使用虚拟线程、`java.net.http.HttpClient` 等新 API |
| 构建工具 | Maven 3.x | 多模块聚合，每个子模块独立 pom.xml |
| Netty | 4.1.104 | 仅用于网络通信层（Web 服务器、注册中心、网关） |
| Jackson | 2.15.2 | 唯一允许的 JSON 序列化库 |
| JUnit | 5.10.0 | 唯一允许的测试框架 |
| SLF4J + Logback | 2.0.9 / 1.4.11 | 唯一允许的日志方案 |
| CGLIB | 3.3.0 | AOP 动态代理（无接口类） |
| ASM | 9.6 | 字节码操作（配合 CGLIB） |
| Protobuf | 3.24.0 | 可选序列化方案（Feign 模块） |

### 2.1 禁止项（硬性要求）

- **严禁**引入任何 Spring 全家桶依赖（spring-core、spring-boot-starter 等）
- **严禁**引入第三方 IOC/AOP 框架（Guice、Dagger 等）
- **严禁**引入成熟服务治理组件（Eureka、Nacos、Hystrix、Sentinel 等）
- **严禁**在代码中写"面试相关"注释（考点说明统一放在 INTERVIEW_QUESTIONS.md）
- 核心逻辑（IOC、AOP、注册发现、负载均衡、熔断器）必须 **从零自研**

---

## 3. 模块架构与依赖关系

### 3.1 模块总览（10 个子模块）

```
boot-cloud-parent (聚合父)
├── mini-spring-core          # 核心容器层：IOC + AOP + 事件
├── mini-spring-boot          # Boot 启动层：自动配置 + 嵌入式 Netty Web 服务器
├── mini-spring-cloud-registry # 微服务层：服务注册与发现
├── mini-spring-cloud-feign   # 微服务层：声明式 HTTP 客户端
├── mini-spring-cloud-loadbalancer # 微服务层：客户端负载均衡
├── mini-spring-cloud-circuitbreaker # 微服务层：熔断器
├── mini-spring-gateway       # 微服务层：API 网关
├── jvm-optimizer             # 性能优化层：JVM 监控与调优
├── concurrent-optimizer      # 性能优化层：线程池 + 锁 + AQS/CAS
└── demo-app                  # 示例应用层：user/order/goods 三个微服务
```

### 3.2 模块依赖图（单向依赖，严禁循环）

```
demo-app → 所有其他模块
mini-spring-boot → mini-spring-core
mini-spring-gateway → mini-spring-boot, mini-spring-cloud-registry
mini-spring-cloud-feign → mini-spring-cloud-registry, mini-spring-cloud-loadbalancer
mini-spring-cloud-loadbalancer → mini-spring-cloud-registry (仅 ServiceInstance 模型)
mini-spring-cloud-circuitbreaker → 无依赖（完全独立）
jvm-optimizer → 无依赖（仅 JDK Management API）
concurrent-optimizer → 无依赖（仅 JDK Concurrent API）
```

### 3.3 各模块核心类与职责

#### mini-spring-core（28 个类/接口/注解）
- `BeanDefinition` — Bean 元数据封装（类、实例、构造器、注入字段、生命周期方法）
- `BeanFactory` / `DefaultListableBeanFactory` — IOC 容器核心，三级缓存解决循环依赖，per-bean-lock 并发安全
- `GenericApplicationContext` — 应用上下文，组合 BeanFactory，管理事件监听器
- `ClassPathBeanDefinitionScanner` — 类路径扫描，解析 `@Component`/`@Service`/`@Repository`/`@Controller`
- `ProxyFactory` — 自动选择 JDK 动态代理或 CGLIB 代理
- `AspectJAwareAdvisorAutoProxyCreator` — 切面扫描与代理创建
- 自定义注解：`@Component`, `@Service`, `@Repository`, `@Controller`, `@Autowired`, `@Scope`, `@PostConstruct`, `@PreDestroy`, `@Aspect`, `@Before`, `@After`, `@Around`, `@Pointcut`
- 事件体系：`ApplicationEvent`, `ApplicationListener<E>`

#### mini-spring-boot（20 个类）
- `SpringApplication` — 启动入口（run 方法），创建上下文 → 扫描 → 加载自动配置 → refresh
- `BootApplication` — 复合注解（`@EnableAutoConfiguration` + `@ComponentScan`）
- `AutoConfigurationLoader` — 读取 `META-INF/spring.factories`，按条件装配 Bean
- `NettyWebServer` — 嵌入式 HTTP 服务器（Boss 1 + Worker NIO）
- `HttpRequestHandler` — Netty Handler，路由注册 + 请求分发 + JSON 序列化
- 条件注解：`@ConditionalOnClass`, `@ConditionalOnProperty`
- Web 注解：`@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@RequestBody`, `@PathVariable`

#### mini-spring-cloud-registry（10 个类）
- `ServiceRegistry` / `InMemoryServiceRegistry` — 服务注册/注销/续约，ConcurrentHashMap 双存储（serviceStore + instanceStore），定时健康检查
- `ServiceDiscovery` / `DefaultServiceDiscovery` — 服务发现，本地缓存 + 定时刷新
- `RegistryServer` — 独立注册中心 HTTP 服务（Netty，端口 8081）
- `ServiceInstance` — 服务实例模型（serviceId, instanceId, host, port, healthy, heartbeat）

#### mini-spring-cloud-loadbalancer（8 个类）
- `LoadBalancer` 接口 + 4 种实现：
  - `RoundRobinLoadBalancer` — `AtomicInteger` 轮询
  - `RandomLoadBalancer` — `ThreadLocalRandom` 随机
  - `WeightedRoundRobinLoadBalancer` — metadata weight 加权轮询
  - `LeastActiveLoadBalancer` — 最少活跃数（需外部调用 `completeRequest`）
- `LoadBalancerFactory` — 策略工厂，按名称创建

#### mini-spring-cloud-feign（15 个类）
- `FeignClientFactory` — JDK 动态代理工厂，`ConcurrentHashMap` 缓存
- `FeignInvocationHandler` — 拦截方法调用 → 构建 HTTP 请求 → 调用后端 → 解码响应
- 编解码器：`JsonEncoder/Decoder`（Jackson）、`ProtobufEncoder/Decoder`
- 集成 `ServiceDiscovery` + `LoadBalancer` 实现服务名解析

#### mini-spring-cloud-circuitbreaker（8 个类）
- `CircuitBreaker` / `DefaultCircuitBreaker` — 状态机（CLOSED → OPEN → HALF_OPEN → CLOSED）
- `CircuitBreakerConfig` — Builder 模式配置（failureThreshold, timeout, resetTimeout）
- `CircuitBreakerFactory` — 按 name 缓存单例
- `CircuitBreakerOpenException` — 熔断打开时抛出的异常

#### mini-spring-gateway（路由 + 过滤器链）
- `Gateway` / `DefaultGateway` — 路由转发引擎
- `Route` — 路由定义（path, serviceId, url, order, enabled）
- 过滤器链（Chain of Responsibility）：`GatewayFilter` + `GatewayFilterChain`
- 内置过滤器：`LoggingFilter`, `RateLimitFilter`（基于 AtomicInteger 的简单限流）
- `doForward()` 当前为 **stub 实现**，未真正转发 HTTP 请求

#### jvm-optimizer
- `JVMInfo` — 通过 MXBeans 获取内存、线程、GC、编译等运行时信息
- `JVMProfiler` — 定时打印 JVM 状态报告
- `GCTuner` — GC 统计算法检测 + 调优建议
- `MemoryLeakSimulator` — 模拟/演示 OOM 场景

#### concurrent-optimizer
- `VirtualThreadPool` — 基于 JDK 21+ `Executors.newVirtualThreadPerTaskExecutor()`
- `TraditionalThreadPool` — 自定义 `ThreadPoolExecutor`，CPU/IO 自适应池
- `ThreadPoolFactory` — 统一工厂（`TaskType.CPU_INTENSIVE / IO_INTENSIVE / VIRTUAL`）
- `AqsUtils.SimpleAQS` — 简化版 AQS 实现（独占 + 共享模式）
- `ReentrantLock` / `CountDownLatch` / `Semaphore` — 基于 SimpleAQS 构建
- `CasUtils` — CAS 无锁栈（含 ABA 问题演示与 StampedLock 解法）
- `LockComparator` — 锁性能对比工具
- `DeadlockDetector` — 死锁检测与模拟

#### demo-app
- `Application` — 启动入口，`@SpringBootApplication` + `SpringApplication.run()`
- `UserController / OrderController / ProductController` — REST 接口
- `UserService / OrderService / ProductService` — 业务逻辑（内存存储）
- `UserRepository` — 数据访问层
- `OrderService` 使用 `@CircuitBreaker` 演示熔断

---

## 4. 编码规范（强制执行）

### 4.1 包命名

- 根包：`com.bootcloud.{module}`（如 `com.bootcloud.core`, `com.bootcloud.boot`）
- 子包约定：`annotation`, `bean`, `context`, `aop`, `event`, `factory`, `core`, `core.impl`, `model`, `server`, `filter`, `filter.impl`

### 4.2 类命名

- 接口：简单名词（`BeanFactory`, `LoadBalancer`, `ServiceRegistry`）
- 实现类：`Default` / `Impl` 前缀或具体描述（`DefaultListableBeanFactory`, `InMemoryServiceRegistry`）
- 注解：`@` + 描述性名词（`@ConditionalOnClass`, `@EnableServiceRegistry`）
- 异常：`XxxException`（继承 `RuntimeException`）
- 工具类：`XxxUtils` / `XxxTools`

### 4.3 方法命名

- Bean 创建：`createXxx()`（工厂方法）或 `doCreateXxx()`（模板方法内部步骤）
- 布尔判断：`isXxx()`, `hasXxx()`, `canXxx()`
- 获取单例：`getSingleton()`, `getBean()`
- 注册/注销：`register()`, `deregister()`, `renew()`

### 4.4 注释要求

- **核心代码必须添加注释**，说明底层原理和实现思路
- 每个类顶部注释说明职责和设计意图
- 关键算法/数据结构注释说明时间/空间复杂度
- **不写"面试相关"注释**（原理说明放 INTERVIEW_QUESTIONS.md）
- 注释语言：中文优先，代码内英文注释也可

### 4.5 并发安全约定

- 所有共享可变状态必须使用线程安全数据结构（`ConcurrentHashMap`, `AtomicInteger`, `AtomicReference` 等）
- 复合操作（check-then-act）必须加锁或使用原子操作
- `ConcurrentHashMap` 不等同于线程安全的复合操作
- 优先使用 per-bean-lock 而非全局锁（参考 `DefaultListableBeanFactory` 的 `computeIfAbsent` 锁设计）
- AQS 相关操作必须保证 `LockSupport.park()` / `unpark()` 正确配对

---

## 5. 构建与测试

### 5.1 构建命令

```bash
# 全量编译
mvn clean install

# 跳过测试编译
mvn clean install -DskipTests

# 单个模块编译
mvn clean install -pl mini-spring-core

# 运行指定模块测试
mvn test -pl concurrent-optimizer

# 生成覆盖率报告（JaCoCo）
mvn test
```

### 5.2 测试规范

- 测试文件命名：`*Test.java` 或 `*Tests.java`
- 测试框架：JUnit 5（`@Test`, `@BeforeEach`, `@AfterEach`, `@DisplayName`）
- 目标覆盖率：**≥ 80%**（当前未达标，需持续补充）
- 每个核心类至少有一个对应测试
- 性能测试需包含对比基准（如虚拟线程 vs 传统线程）
- **mini-spring-boot 和 demo-app 目前缺少测试**（TODO P0/P1）

### 5.3 JDK 版本

- 编译目标：JDK 25（`maven.compiler.source=25`, `maven.compiler.target=25`）
- 运行时必须使用 JDK 25+（虚拟线程 API 依赖）

---

## 6. 已知问题与不可改动的底层逻辑

### 6.1 必须保留的设计（不可改动）

- **三级缓存架构**：`singletonObjects` → `earlySingletonObjects` → `singletonFactories`，这是循环依赖解决的核心设计
- **per-bean-lock 并发模型**：`ReentrantLock` 按 bean name 独立，不能用全局锁替换
- **BeanDefinition 结构**：包含 beanName, beanClass, instance, constructor, autowiredFields, postConstructMethods, preDestroyMethods
- **注解元数据体系**：`@Service`/`@Repository`/`@Controller` 均 meta-annotated with `@Component`，扫描时统一按 `@Component` 识别
- **Netty Boss(1) + Worker 线程模型**：嵌入式 Web 服务器的基础架构
- **模块依赖方向**：registry → loadbalancer → feign 单向依赖链不可打破

### 6.2 已知 Bug / 待修复问题（参见 TODO.md）

| 优先级 | 问题 | 影响模块 |
|---|---|---|
| P0 | Web 服务器从未被 `start()`，应用启动后无法接收 HTTP 请求 | mini-spring-boot |
| P0 | `AutoConfigurationLoader` 条件判断运算符优先级错误 | mini-spring-boot |
| P0 | `@PathVariable` 注解已定义但未实现解析 | mini-spring-boot |
| P0 | 熔断器状态转换缺少 `AtomicReference` 原子性保证 | circuitbreaker |
| P1 | `Environment` 是内部类，应抽取为独立类 | mini-spring-boot |
| P1 | 缺少 `@Bean` / `@Configuration` 注解机制 | mini-spring-boot |
| P1 | AOP 未通过 `BeanPostProcessor` 自动织入 Bean 生命周期 | mini-spring-core |
| P1 | AOP 通知执行顺序不正确（@Around 与 @Before/@After 分循环执行） | mini-spring-core |
| P1 | 缺少 `@AfterReturning` / `@AfterThrowing` 通知类型 | mini-spring-core |
| P1 | `RegistryServer` JSON 解析用字符串分割，应改用 Jackson | registry |
| P2 | 网关 `doForward()` 为 stub，未真正转发 HTTP 请求 | gateway |
| P2 | Feign 客户端缓存无过期机制 | feign |
| P2 | 缺少 `@Value` 注解和配置文件加载 | boot |

---

## 7. 开发优先级与路线图

按 TODO.md 定义的开发顺序执行：

```
Phase 1（核心完善 P0）
├── 配置文件支持（@Value、application.properties/yml）
├── BeanPostProcessor 扩展点
├── 事件驱动机制完善（@EventListener、异步事件）
└── 补充 mini-spring-boot 单元测试

Phase 2（微服务增强 P1）
├── 服务注册中心持久化
├── 心跳检测机制完善
├── Feign 客户端增强（超时、重试、连接池）
├── 熔断器指标监控（滑动窗口）
└── 编写集成测试

Phase 3（体验优化 P2）
├── 启动 Banner 与日志优化
├── 异常处理机制（@ExceptionHandler）
├── 参数校验支持（@Valid）
└── 代码注释完善

Phase 4（高级特性 P3）
├── 分布式配置中心
├── 分布式链路追踪（TraceId）
├── 安全认证模块（JWT）
└── 性能基准测试报告（JMH）
```

---

## 8. 设计模式与架构约定

### 8.1 已使用的设计模式

| 模式 | 应用位置 |
|---|---|
| Factory | `BeanFactory`, `LoadBalancerFactory`, `CircuitBreakerFactory`, `ThreadPoolFactory` |
| Delegation | `GenericApplicationContext` 委托给 `BeanFactory` |
| Template Method | `getSingleton(name, ObjectFactory)` |
| Strategy | `ProxyFactory`（JDK vs CGLIB），`LoadBalancer` 多策略 |
| Chain of Responsibility | Gateway 过滤器链 |
| Observer | `ApplicationEvent` + `ApplicationListener` |
| Registry | `BeanDefinitionMap`, `FeignClientFactory` 缓存 |
| Singleton | IOC 容器一级缓存 |

### 8.2 数据流约定

- **Bean 生命周期**：实例化 → 暴露早期引用（循环依赖） → 属性填充（@Autowired） → 初始化（@PostConstruct） → 注册销毁回调（@PreDestroy）
- **AOP 通知执行顺序（期望）**：Around(前置) → Before → 目标方法 → Around(后置) → After → AfterReturning
- **请求处理流**：HttpRequestHandler 接收 Netty 请求 → 匹配路由 → 解析 @RequestBody → 反射调用 → JSON 响应
- **Feign 调用流**：接口方法调用 → JDK Proxy 拦截 → ServiceDiscovery 解析实例 → LoadBalancer 选择 → HTTP 请求 → 解码响应
- **熔断状态机**：CLOSED（正常）→ 失败次数达阈值 → OPEN（拒绝）→ resetTimeout 到 → HALF_OPEN（试探）→ 成功 → CLOSED / 失败 → OPEN

---

## 9. 性能优化约束

### 9.1 虚拟线程使用原则

- **IO 密集型 / 高并发短任务**：优先使用 `VirtualThreadPool`
- **CPU 密集型任务**：使用 `TraditionalThreadPool`（CPU 核心数 + 1）
- 虚拟线程在 CPU 密集场景比传统线程慢约 300%（有实测数据支撑）
- 虚拟线程无需配置池大小，基于 `newVirtualThreadPerTaskExecutor`

### 9.2 锁选择原则

- 读多写少：`ReadWriteLock`
- 简单计数器：`AtomicInteger`
- 复杂同步逻辑：`ReentrantLock`（支持 tryLock、超时、可中断）
- 极短临界区：`synchronized`（JVM 自动锁升级）
- 无锁编程：CAS（注意 ABA 问题，用 `AtomicStampedReference` 解决）

### 9.3 缓存策略

- Bean 缓存：三级 `ConcurrentHashMap`
- 服务发现缓存：`DefaultServiceDiscovery` 本地 `ConcurrentHashMap` + 定时刷新
- Feign 客户端缓存：`ConcurrentHashMap`（**无过期机制，待优化**）

---

## 10. Git 使用规范

- 分支：当前使用 `master` 作为唯一开发分支
- 提交信息：简洁描述变更内容，参考已有提交风格（如 "优化线程池模块：..."、"修复aqs模块的bug"）
- 提交前确保 `mvn clean test` 通过
- 不提交：`target/`、`.idea/`、`.vscode/`、`.trae/`（已在 `.gitignore` 中排除）

---

## 11. 新增模块开发约束

- 必须在 `boot-cloud-parent/pom.xml` 的 `<modules>` 中注册
- 每个模块必须有独立 `pom.xml`，继承父 POM
- 包名必须遵循 `com.bootcloud.{module}` 规范
- 核心接口与实现分离（接口放 `core/` 包，实现放 `core.impl/` 包）
- 必须在 `src/test/` 下提供对应的单元测试
- 必须在 TODO.md 中登记开发状态

---

## 12. 配置文件规范（待实现，P0）

- 配置文件格式：`application.properties` 或 `application.yml`
- 配置位置：类路径 `src/main/resources/`
- 多环境支持：`application-{profile}.properties`
- 占位符语法：`${key}` 或 `${key:defaultValue}`
- 注解：`@Value("${key}")` 注入

---

> **文档维护**：每次完成新功能模块或修复 Bug 后，同步更新本文档和 TODO.md。
> **最后更新**：2026-05-01
