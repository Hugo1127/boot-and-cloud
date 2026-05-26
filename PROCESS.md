# Boot&Cloud 项目状态

> 简要记录已完成工作，重点追踪待解决问题。最后更新：2026-05-26

---

## 一、当前状态概览

**构建**：`mvn clean install` 全量通过，11 个模块共 191 个测试，0 失败。

| 模块 | 测试类 | 测试数 | 状态 |
|------|--------|--------|------|
| mini-spring-core | 9 | 41 | ✅ 最完整 |
| mini-spring-boot | 0 | 0 | ❌ 无测试 |
| mini-spring-cloud-registry | 2 | 11 | ⚠️ 有 Bug |
| mini-spring-cloud-feign | 3 | 18 | ⚠️ 功能不足 |
| mini-spring-cloud-loadbalancer | 1 | 7 | ✅ |
| mini-spring-cloud-circuitbreaker | 1 | 10 | ✅ |
| mini-spring-gateway | 1 | 6 | ⚠️ 转发为桩代码 |
| mini-spring-cloud-mq | 0 | 0 | ❌ 空模块 |
| jvm-optimizer | 2 | 9 | ✅ |
| concurrent-optimizer | 8 | 89 | ✅ 测试最充分 |
| demo-app | 0 | 0 | ❌ 无测试 |

### 已完成功能（简要）

- **IOC 容器**：Bean 扫描/注册、三级缓存循环依赖、字段/Setter/构造器注入、@PostConstruct/@PreDestroy 生命周期、Singleton/Prototype 作用域、per-bean-lock 并发安全
- **AOP**：JDK/CGLIB 代理、@Before/@After/@Around 通知、@Pointcut 切点表达式
- **Boot 启动**：SpringApplication.run()、自动配置加载、@ConditionalOnClass/OnProperty、Netty 嵌入式服务器、@RequestMapping/@GetMapping/@PostMapping、@RequestBody 绑定
- **微服务组件**：内存注册中心 + HTTP RegistryServer、4 种负载均衡策略、Feign 声明式调用（动态代理 + 服务发现集成）、三态熔断器、API 网关（路由 + 过滤器链）
- **性能工具**：JVM 信息采集/GC 调优/内存泄漏模拟、虚拟线程池/传统线程池/锁对比/死锁检测/AQS/CAS/信号量

---

## 二、待修复项（按优先级）

### P0 — 核心功能缺失或不可用

| # | 问题 | 模块 | 说明 |
|---|------|------|------|
| 1 | **mini-spring-cloud-mq 空模块** | mq | 零 Java 源文件，仅 pom.xml 和目录骨架。Exchange/Queue/Broker/ACK/DLQ 全部未实现 |
| 2 | **@PathVariable 未实现运行时解析** | boot | 注解已定义，但 HttpRequestHandler 不解析 `{id}` 路径变量，也不注入方法参数 |
| 3 | **@Value / 配置文件缺失** | boot | 无 application.properties 加载，无 @Value 注解，Environment 只有硬编码默认值 |
| 4 | **AOP 未集成到 Bean 生命周期** | core | 需手动调用 ProxyFactory.wrapIfNecessary()，缺少 BeanPostProcessor 自动代理机制 |
| 5 | **AOP 通知执行顺序不正确** | core | @Before/@Around/@After 分开循环执行，非责任链模式，@Around 无法包裹 @Before |
| 6 | **RegistryServer JSON 解析用字符串拆分** | registry | 手动 split(",") + split("=") 解析，标准 JSON 客户端无法对接，应改用 Jackson |
| 7 | **mini-spring-boot 零测试** | boot | 嵌入式服务器、自动配置、HTTP 请求分发全部无测试覆盖 |

### P1 — 重要功能缺失

| # | 问题 | 模块 | 说明 |
|---|------|------|------|
| 8 | **DefaultGateway.doForward() 为桩代码** | gateway | 返回字符串而非实际转发 HTTP 请求，网关核心功能不可用 |
| 9 | **缺少 @AfterReturning / @AfterThrowing** | core | 异常和返回通知未实现 |
| 10 | **缺少 BeanPostProcessor 机制** | core | 无 postProcessBeforeInitialization / postProcessAfterInitialization 扩展点 |
| 11 | **Feign 无超时/重试/拦截器** | feign | 无 connectTimeout/readTimeout 配置，无 Retryer，无 RequestInterceptor 链 |
| 12 | **ClassPathBeanDefinitionScanner 仅支持文件系统** | core | 使用 java.io.File 扫描，打成 JAR 后类扫描失效 |
| 13 | **缺少 spring.factories 文件** | boot | AutoConfigurationLoader 期望读取 META-INF/spring.factories，但项目中没有此文件，自动配置静默跳过 |
| 14 | **demo-app 零测试** | demo | 控制器、服务层全部无测试 |
| 15 | **Environment 为内部类** | boot | 应抽取为独立类，方便扩展配置源 |

### P2 — 体验与健壮性

| # | 问题 | 模块 | 说明 |
|---|------|------|------|
| 16 | 缺少全局异常处理器（@ExceptionHandler / @RestControllerAdvice） | boot | 统一错误响应格式缺失 |
| 17 | 缺少参数校验支持（@Valid / @NotNull / @Size） | boot | |
| 18 | 心跳检测无客户端主动上报 | registry | 仅服务端被动超时剔除 |
| 19 | 服务发现无本地缓存刷新机制 | registry | |
| 20 | Feign 客户端缓存无过期策略 | feign | ConcurrentHashMap 永不过期 |
| 21 | 切点表达式仅支持类名.方法名 | core | 不支持 execution()、@annotation() 等标准语法 |
| 22 | 缺少事件驱动机制完善 | core | @EventListener、异步事件、内置 ApplicationReadyEvent |
| 23 | 注册中心无持久化 | registry | 纯内存存储，重启丢失 |

---

## 三、开发建议顺序

```
Phase 1（核心修复 — P0 优先）
├── @PathVariable 运行时解析
├── @Value + application.properties 加载
├── BeanPostProcessor 实现 + AOP 自动代理集成
├── AOP 通知执行顺序重构（责任链模式）
├── @AfterReturning / @AfterThrowing
├── RegistryServer 改用 Jackson 解析 JSON
├── mini-spring-boot 补充单元测试
└── mini-spring-cloud-mq Phase 1：Message/Exchange/Queue/Broker

Phase 2（功能补全 — P1）
├── MQ Phase 2：ACK/DLQ/幂等/重试
├── MQ Phase 3：Pull/Push/ConsumerGroup/Offset
├── MQ Phase 4：@MqListener/@EnableMq/自动配置
├── DefaultGateway 实现真正 HTTP 转发
├── Feign 超时/重试/拦截器
├── spring.factories 文件 + 自动配置验证
├── 事件机制完善
└── demo-app 补充测试

Phase 3（体验优化 — P2）
├── 全局异常处理
├── 参数校验
├── 切点表达式增强
├── 心跳客户端上报
├── 注册中心持久化
└── Environment 抽取为独立类
```


