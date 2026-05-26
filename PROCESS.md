# Boot&Cloud 项目完善计划

> 本文档记录项目当前状态、待完善模块、待解决问题及后续开发建议
> 生成时间：2026-04-19
> 最后更新：2026-05-26

***

## 一、已完成工作

### 1.1 核心架构层

- ✅ IOC 容器完整实现（BeanFactory、三级缓存、循环依赖解决）
- ✅ AOP 框架实现（JDK/CGLIB 代理、@Aspect/@Before/@After/@Around）
- ✅ Bean 生命周期管理（@PostConstruct、@PreDestroy）
- ✅ Bean 作用域支持（Singleton、Prototype）
- ✅ 并发安全设计（per-bean-lock、ConcurrentHashMap）
- ✅ 依赖注入支持：字段注入、Setter 注入、构造器注入
- ✅ Bean 按类型查找（byType + byName 回退策略）

### 1.2 Boot 启动层

- ✅ SpringApplication 启动类
- ✅ 自动配置加载机制（spring.factories）
- ✅ 条件注解（@ConditionalOnClass、@ConditionalOnProperty）
- ✅ Netty 嵌入式 Web 服务器
- ✅ 请求映射（@RequestMapping、@GetMapping、@PostMapping）
- ✅ @RequestBody 参数绑定
- ✅ Environment 环境配置（默认 server.port=8080）

### 1.3 微服务组件层

- ✅ 服务注册与发现（InMemoryServiceRegistry、DefaultServiceDiscovery）
- ✅ 注册中心 HTTP 服务（RegistryServer）
- ✅ 负载均衡（4 种策略：轮询、随机、加权轮询、最少活跃）
- ✅ Feign 声明式调用（动态代理、JSON/Protobuf 编解码）
- ✅ 熔断器（CLOSED/OPEN/HALF_OPEN 三态转换）
- ✅ API 网关（路由、过滤器链、限流）
- ✅ Feign 与服务注册中心集成

### 1.4 性能优化层

- ✅ JVM 信息获取（内存、线程、GC、运行时）
- ✅ GC 调优建议生成器
- ✅ 内存泄漏模拟器
- ✅ 虚拟线程池（JDK 25）
- ✅ 传统线程池（CPU/IO 自适应）
- ✅ 锁性能对比工具
- ✅ 死锁检测器
- ✅ AQS/CAS/信号量工具类
- ✅ 并发工具（CountDownLatch、CyclicBarrier、BlockingQueue）

### 1.5 文档与测试

- ✅ README.md（完整使用说明）
- ✅ docs/architecture.md（架构设计文档）
- ✅ docs/interview-questions.md（面试题总结，29 题）
- ✅ CLAUDE.md（开发规范）
- ✅ docs/agents.md（AI Agent 提示词）
- ✅ 消息队列基础模块（Message、Exchange、Queue、Broker）
- ✅ 26 个单元测试类覆盖各核心模块

### 1.6 测试覆盖统计

| 模块                               | 测试类数 | 主要测试内容                           |
| -------------------------------- | ----- | -------------------------------- |
| mini-spring-core                 | 9     | Bean 注册、生命周期、循环依赖、DI、AOP、作用域、切点表达式 |
| mini-spring-boot                 | 0     | 全部测试缺失                             |
| mini-spring-cloud-registry       | 2     | 服务注册、服务发现                          |
| mini-spring-cloud-feign          | 3     | Feign 调用、序列化性能、服务发现集成              |
| mini-spring-cloud-loadbalancer   | 1     | 4 种负载均衡策略                          |
| mini-spring-cloud-circuitbreaker | 1     | 熔断器状态转换                            |
| mini-spring-gateway              | 1     | 网关路由与过滤器                           |
| mini-spring-cloud-mq             | 0     | 全部测试缺失（新模块）                        |
| jvm-optimizer                    | 2     | JVM 信息获取、GC 调优                    |
| concurrent-optimizer             | 7     | AQS、CAS、信号量、死锁检测、锁对比、线程池、并发工具     |
| demo-app                         | 0     | 全部测试缺失                             |

***

## 二、待完成模块（按优先级排序）

### 优先级 P0：核心功能缺失

#### 2.1 配置文件支持

**核心功能点**：

- 实现 `application.properties` / `application.yml` 文件读取
- 支持 `@Value` 注解注入配置值
- 支持配置占位符解析（`${server.port}`）
- 支持多环境配置（application-dev.properties、application-prod.properties）

**交付要求**：

- 在 `mini-spring-boot` 中扩展
- 实现 `PropertySource` 抽象类
- 实现 `@Value` 注解处理器
- 单元测试覆盖率 ≥ 80%

**面试考点**：

- Spring Boot 配置加载顺序
- @Value 与 @ConfigurationProperties 的区别
- 配置优先级规则

***

#### 2.2 @PathVariable 注解实现

**当前状态**：注解已定义（`mini-spring-boot/web/annotation/PathVariable.java`），但 HttpRequestHandler 中未实现解析逻辑

**核心功能点**：

- 在 HttpRequestHandler 中实现路径变量匹配与提取
- 支持 `/api/user/{id}` 格式的路径
- 将路径变量绑定到方法参数

**交付要求**：

- 修改 `HttpRequestHandler.findHandler()` 支持路径模板匹配
- 实现 `resolveArguments()` 中的 @PathVariable 解析
- 单元测试验证路径变量提取

**面试考点**：

- URL 路径模式匹配原理
- 正则表达式在路由中的应用

***

#### 2.3 Web 服务器启动集成

**当前状态**：✅ 已实现

**已实现功能点**：

- ✅ 在 `SpringApplication.run()` 末尾通过 `WebServerLifecycle` 自动启动 Web 服务器
- ✅ 支持通过配置文件自定义端口（通过 `Environment`）
- ✅ WebServer 在独立线程中启动，不阻塞主线程
- ✅ 实现应用关闭钩子（shutdown hook）

**修改文件**：

- `SpringApplication.java`：添加 `startWebServers()` 方法
- `WebServerLifecycle.java`：新增生命周期管理类
- `ApplicationContext.java`：新增 `getBeansOfType()` 接口方法
- `BeanFactory.java`：新增 `getBeansOfType()` 接口方法
- `DefaultListableBeanFactory.java`：实现 `getBeansOfType()`
- `GenericApplicationContext.java`：委托实现 `getBeansOfType()`
- `AutoConfigurationLoader.java`：修复运算符优先级 Bug 和方法参数解析

**面试考点**：

- 嵌入式容器启动原理
- JVM 关闭钩子的作用

***

### 优先级 P1：重要功能增强

#### 2.4 事件驱动机制完善

**核心功能点**：

- 实现 `@EventListener` 注解
- 实现异步事件发布（@Async 支持）
- 实现事件监听器排序（@Order）
- 实现内置事件（ApplicationStartedEvent、ApplicationReadyEvent）

**交付要求**：

- 扩展 `ApplicationEvent` 体系
- 实现事件发布器 `ApplicationEventMulticaster`
- 支持同步/异步事件分发
- 单元测试覆盖事件生命周期

**面试考点**：

- 观察者模式在 Spring 中的应用
- 异步事件处理原理
- 事件发布与事务的关系

***

#### 2.5 BeanPostProcessor 扩展点

**核心功能点**：

- 实现 `BeanPostProcessor` 接口
- 支持 `postProcessBeforeInitialization` 和 `postProcessAfterInitialization`
- 实现 AOP 自动代理创建器（基于 BeanPostProcessor）
- 支持自定义 BeanPostProcessor 注册

**交付要求**：

- 在 `DefaultListableBeanFactory` 中集成 BeanPostProcessor 链
- 重构 AOP 模块使用 BeanPostProcessor 织入代理
- 提供自定义 BeanPostProcessor 示例
- 单元测试验证执行顺序

**面试考点**：

- BeanPostProcessor 的执行时机
- AOP 代理如何通过 BeanPostProcessor 创建
- 常见 BeanPostProcessor 实现类

***

#### 2.6 AOP 通知执行顺序重构

**当前状态**：ProxyFactory 中 @Before/@Around/@After 分开循环执行，破坏了责任链模式

**正确顺序**（Spring AOP）：
```
Around(前置) → Before → 目标方法 → Around(后置) → After → AfterReturning
```

**交付要求**：

- 使用责任链模式（AdviceChain）统一执行
- 添加 @AfterReturning 和 @AfterThrowing 注解
- 确保 @Around 能包裹 @Before 和 @After
- 单元测试验证执行顺序

**面试考点**：

- 拦截器链模式设计
- AOP 通知执行顺序原理

***

#### 2.7 服务注册中心持久化

**核心功能点**：

- 当前 `InMemoryServiceRegistry` 为纯内存存储，重启丢失
- 支持文件存储或轻量级数据库（H2/SQLite）
- 实现服务实例持久化与恢复
- 支持集群模式下的数据同步

**交付要求**：

- 实现 `PersistentServiceRegistry` 接口
- 提供文件存储实现
- 实现启动时数据恢复
- 性能测试验证读写延迟

**面试考点**：

- 注册中心数据一致性保证
- 持久化方案选型
- CAP 理论在持久化中的应用

***

#### 2.8 心跳检测机制完善

**核心功能点**：

- 当前心跳检测逻辑不完整
- 实现客户端定时发送心跳
- 实现服务端心跳超时剔除（默认 90 秒）
- 实现自我保护机制（Eureka 风格）

**交付要求**：

- 实现 `HeartbeatScheduler` 定时任务
- 实现服务端心跳超时检测线程
- 实现自我保护阈值配置
- 单元测试模拟心跳超时场景

**面试考点**：

- 心跳间隔与超时时间设计
- 自我保护机制原理
- 服务下线与优雅停机

***

#### 2.9 Feign 客户端增强

**核心功能点**：

- 实现连接池管理（HttpClient/OkHttp）
- 实现超时配置（connectTimeout、readTimeout）
- 实现重试机制（Retryer）
- 实现请求/响应拦截器

**交付要求**：

- 实现 `FeignClientConfig` 配置类
- 实现 `Retryer` 接口及默认实现
- 实现 `RequestInterceptor` 链
- 集成测试验证超时与重试

**面试考点**：

- HTTP 连接池原理
- 重试策略设计（指数退避）
- 超时时间设置原则

***

#### 2.10 熔断器指标监控

**核心功能点**：

- 实现熔断器状态指标采集
- 实现请求成功率统计
- 实现慢调用比例统计
- 实现指标导出接口

**交付要求**：

- 实现 `CircuitBreakerMetrics` 类
- 支持滑动窗口统计（时间窗口/计数窗口）
- 实现指标查询 API
- 单元测试验证统计准确性

**面试考点**：

- 滑动窗口算法
- 指标采集对性能的影响
- 熔断器与限流器的区别

***

### 优先级 P2：体验优化

#### 2.11 启动 Banner 与日志优化

**核心功能点**：

- 实现启动 Banner 打印
- 实现日志级别动态配置
- 实现结构化日志输出
- 实现启动耗时统计

***

#### 2.12 异常处理机制

**核心功能点**：

- 实现全局异常处理器（@ExceptionHandler）
- 实现统一错误响应格式
- 实现异常分类（业务异常、系统异常）
- 实现异常日志记录

**交付要求**：

- 实现 `@RestControllerAdvice` 注解
- 实现 `ErrorResponse` 标准格式
- 提供异常处理示例
- 单元测试覆盖异常场景

***

#### 2.13 参数校验支持

**核心功能点**：

- 实现 `@Valid` / `@Validated` 注解
- 实现常用校验注解（@NotNull、@Size、@Pattern）
- 实现校验异常统一处理
- 实现自定义校验器

***

### 优先级 P3：高级特性

#### 2.14 消息队列（mini-spring-cloud-mq）

**设计文档**：`docs/mq-design.md`

**核心功能点**：

- 实现消息路由（Direct/Topic/Fanout 三种 Exchange）
- 实现生产/消费模型（Pull 拉取 + Push 推送）
- 实现消息确认机制（ACK/NACK + 自动重试）
- 实现死信队列（DLQ，超过最大重试次数的消息）
- 实现幂等性检查（布隆过滤器防止重复消费）
- 实现消费者组（参考 Kafka，组内单消费、组间独立）
- 实现偏移量管理（OffsetManager）
- 注解驱动（@MqListener、@EnableMq）
- Boot 自动配置集成

**交付要求**：

- 新模块 `mini-spring-cloud-mq`
- 依赖 `mini-spring-boot`、`concurrent-optimizer`（复用 `MyBlockingQueue`）
- 不依赖任何外部中间件
- 单元测试覆盖 ≥ 80%

**面试考点**：

- 消息队列如何防止重复消费
- ACK 机制与消息可靠性保证
- 死信队列的作用与触发条件
- 布隆过滤器原理与误判率
- Kafka 消费者组与 RabbitMQ Exchange 的区别
- 消息顺序性保证

***

#### 2.15 分布式配置中心

**核心功能点**：

- 实现配置中心服务端
- 实现配置动态刷新（@RefreshScope）
- 实现配置版本管理
- 实现配置变更通知

***

#### 2.16 分布式链路追踪

**核心功能点**：

- 实现 TraceId 生成与传递
- 实现 Span 记录
- 实现链路数据导出
- 实现链路可视化（可选）

***

#### 2.17 安全认证模块

**核心功能点**：

- 实现 JWT Token 生成与验证
- 实现请求拦截与权限校验
- 实现 @PreAuthorize 注解
- 实现登录认证流程

***

## 三、待解决问题

### 3.1 代码质量问题

| 问题描述                                    | 影响模块                       | 优先级 | 解决方案                     |
| --------------------------------------- | -------------------------- | --- | ------------------------ |
| RegistryServer 使用字符串解析 JSON，应使用 Jackson | mini-spring-cloud-registry | P0  | 替换为 Jackson ObjectMapper |
| mini-spring-boot 模块缺少单元测试               | mini-spring-boot           | P0  | 补充启动流程、自动配置测试            |
| demo-app 模块缺少单元测试                       | demo-app                   | P1  | 补充控制器、服务层测试              |
| 部分类注释覆盖率不足 70%                          | 全局                         | P1  | 补充类/方法级注释                |
| 缺少集成测试（跨模块）                             | 全局                         | P1  | 编写端到端集成测试                |
| **Web 服务器从未被 start()**，应用启动后无法接收 HTTP 请求 | ~~mini-spring-boot~~ | ~~P0~~ | ~~在 SpringApplication.run() 末尾调用 server.start()~~ ✅ 已修复 |
| **@PathVariable 注解已定义但未实现解析**，URL 路径变量无法提取 | mini-spring-boot | P0 | 在 HttpRequestHandler 中实现路径变量匹配与提取 |
| **缺少 @Value 注解和配置文件加载**，配置值只能硬编码 | mini-spring-boot | P0 | 实现 PropertySource + @Value 注解处理器 |

### 3.2 架构设计问题

| 问题描述                                     | 影响      | 优先级 | 解决方案                    |
| ---------------------------------------- | ------- | --- | ----------------------- |
| Environment 为内部类，应独立为模块                  | 配置扩展困难  | P1  | 抽取为独立类                  |
| ~~WebServerAutoConfiguration 缺少 @Bean 注解机制~~ | ~~自动配置不完整~~ | ~~P0~~ | ~~实现 @Bean 注解处理~~ ✅ 已修复 |
| 缺少应用关闭钩子完整实现                             | 资源泄漏风险  | P1  | 完善 registerShutdownHook |
| Feign 客户端缓存无过期机制                         | 内存泄漏风险  | P2  | 实现缓存过期策略                |

### 3.3 性能优化问题

| 问题描述                                 | 影响     | 优先级 | 解决方案               |
| ------------------------------------ | ------ | --- | ------------------ |
| BeanFactory 使用 ReentrantLock，可优化为分段锁 | 高并发性能  | P2  | 实现分段锁或 CAS 优化      |
| 服务发现无本地缓存刷新机制                        | 网络开销   | P1  | 实现定时刷新缓存           |
| 熔断器状态转换无原子性保证                        | 并发安全问题 | P0  | 使用 AtomicReference |
| 网关过滤器链执行无超时控制                        | 请求阻塞   | P1  | 实现过滤器超时机制          |

### 3.4 AOP 设计问题

| 问题描述                                    | 影响模块 | 优先级 | 解决方案                    |
| --------------------------------------- | ---- | --- | ----------------------- |
| AOP 未集成到 Bean 生命周期中，需手动调用 wrapIfNecessary() | mini-spring-core | P0 | 实现 BeanPostProcessor 自动代理 |
| 通知执行顺序不正确，@Around 与 @Before/@After 分开执行 | mini-spring-core | P0 | 使用责任链模式统一执行              |
| 切点表达式过于简化，仅支持 "类名.方法名" 匹配            | mini-spring-core | P2 | 支持 execution()、@annotation() 等 |
| 缺少 @AfterReturning 和 @AfterThrowing       | mini-spring-core | P1 | 补充注解与实现                 |

### 3.5 测试覆盖问题

| 模块                               | 当前测试数 | 目标覆盖率 | 缺失测试                   |
| -------------------------------- | ----- | ----- | ---------------------- |
| mini-spring-core                 | 9     | 80%   | BeanPostProcessor、事件机制 |
| mini-spring-boot                 | 0     | 80%   | 全部测试缺失                 |
| mini-spring-cloud-registry       | 2     | 80%   | 心跳检测、持久化               |
| mini-spring-cloud-feign          | 3     | 80%   | 重试、超时、拦截器              |
| mini-spring-cloud-loadbalancer   | 1     | 80%   | 加权轮询分布测试               |
| mini-spring-cloud-circuitbreaker | 1     | 80%   | 并发场景、指标统计              |
| mini-spring-gateway              | 1     | 80%   | 过滤器链、限流                |
| jvm-optimizer                    | 2     | 80%   | 内存泄漏模拟                 |
| concurrent-optimizer             | 7     | 80%   | 边界条件测试                 |
| demo-app                         | 0     | 80%   | 全部测试缺失                 |
| mini-spring-cloud-mq             | 0     | 80%   | 全部测试缺失（新模块）           |

***

## 六、设计约束（不可改动）

> 以下为核心设计决策，后续开发**必须保留**，不可随意重构或删除。

| 约束 | 说明 |
|---|---|
| **三级缓存架构** | `singletonObjects` → `earlySingletonObjects` → `singletonFactories`，循环依赖解决核心设计 |
| **per-bean-lock 并发模型** | `ReentrantLock` 按 bean name 独立，不能用全局锁替换 |
| **BeanDefinition 结构** | 包含 beanName, beanClass, instance, constructor, autowiredFields, autowiredMethods, postConstructMethods, preDestroyMethods |
| **注解元数据体系** | `@Service`/`@Repository`/`@Controller` 均 meta-annotated with `@Component`，扫描时统一按 `@Component` 识别 |
| **Netty Boss(1) + Worker 线程模型** | 嵌入式 Web 服务器的基础架构 |
| **模块依赖方向** | registry → loadbalancer → feign 单向依赖链不可打破 |

***

## 四、后续开发建议

### 4.1 开发顺序建议

```
Phase 1（核心完善）
├── 配置文件支持（@Value、application.properties）
├── @PathVariable 路径变量解析
├── Web 服务器自动启动
├── BeanPostProcessor 扩展点
├── AOP 集成到 Bean 生命周期（自动代理）
├── AOP 通知执行顺序重构（责任链模式）
├── @AfterReturning / @AfterThrowing 注解
└── 补充 mini-spring-boot 单元测试

Phase 2（微服务增强）
├── 服务注册中心持久化
├── 心跳检测机制完善
├── Feign 客户端增强（超时、重试、连接池）
├── 熔断器指标监控
└── 编写集成测试

Phase 3（体验优化）
├── 启动 Banner 与日志优化
├── 异常处理机制
├── 参数校验支持
├── 切点表达式增强（execution、@annotation）
└── 代码注释完善

Phase 4（高级特性）
├── 消息队列（mini-spring-cloud-mq）
│   ├── Phase 1：核心模型（Message/Exchange/Queue/Broker）
│   ├── Phase 2：可靠性保障（ACK/DLQ/幂等/重试）
│   ├── Phase 3：消费者模型（Pull/Push/ConsumerGroup/Offset）
│   ├── Phase 4：Boot 集成（@MqListener/@EnableMq/自动配置）
│   └── Phase 5：网络通信层（Netty Server/Client，可选）
├── 分布式配置中心
├── 分布式链路追踪
├── 安全认证模块
└── 性能基准测试报告
```

### 4.2 性能测试建议

| 测试场景       | 工具     | 目标指标                |
| ---------- | ------ | ------------------- |
| IOC 容器启动耗时 | JMH    | < 500ms（100 个 Bean） |
| AOP 代理调用开销 | JMH    | < 1ms/次             |
| 服务注册吞吐量    | JMeter | > 1000 QPS          |
| Feign 调用延迟 | JMeter | < 50ms（P99）         |
| 熔断器状态切换    | 单元测试   | < 1ms               |
| 网关转发延迟     | JMeter | < 10ms              |

### 4.3 代码质量提升建议

1. **引入静态代码分析工具**
   - SonarQube 或 Checkstyle
   - 配置代码规范检查规则
   - 设置质量门禁（Bug 数、漏洞数、代码异味）
2. **完善 CI/CD 流程**
   - GitHub Actions / Gitee Go 自动化构建
   - 每次提交自动运行测试
   - 生成测试覆盖率报告
3. **文档完善**
   - 每个模块添加 USAGE.md 使用说明
   - 补充 API 文档（Javadoc）
   - 更新架构文档（包含新模块）

### 4.4 面试考点绑定建议

| 模块                | 绑定考点            | 代码位置                        |
| ----------------- | --------------- | --------------------------- |
| 配置文件              | @Value 原理、配置优先级 | PropertyResolver            |
| BeanPostProcessor | AOP 代理创建时机      | BeanPostProcessor           |
| 事件机制              | 观察者模式、异步事件      | ApplicationEventMulticaster |
| 心跳检测              | 服务健康检查、自我保护     | HeartbeatScheduler          |
| 熔断器指标             | 滑动窗口、统计算法       | CircuitBreakerMetrics       |
| 分布式追踪             | TraceId 传递、采样率  | Tracer                      |

***

## 五、补充说明

### 5.1 项目优势

- 架构清晰，分层合理
- 核心功能完整（IOC/AOP/微服务）
- 面试导向，考点明确
- 代码规范，注释详细
- 测试覆盖较全面（26 个测试类）

### 5.2 改进重点

- 配置文件支持（当前硬编码）
- 单元测试补充（mini-spring-boot、demo-app）
- 注册中心持久化（当前纯内存）
- AOP 自动代理集成（当前需手动调用）
- 集成测试（跨模块端到端）
- 性能基准测试（JMH）

### 5.3 风险提示

- JDK 25 版本较新，部分 API 可能不稳定
- 虚拟线程性能数据依赖具体硬件环境
- 内存注册中心不适用于生产环境

***

**文档维护**：每次完成模块后更新本文档
**最后更新**：2026-05-26