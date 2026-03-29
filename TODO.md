# Boot&Cloud 项目日志

## 项目概述

Boot&Cloud是一个从零手写的极简Java微服务框架，深度复刻Spring Boot + Spring Cloud核心能力，集成JVM调优、多线程与锁优化模块，全程以"面试备战"为核心。

## 已完成模块

### ✅ 核心容器层（100%）

#### mini-spring-core
- **IOC容器实现**
  - 自定义注解：`@Component`, `@Service`, `@Repository`, `@Controller`, `@Autowired`
  - 核心接口：`BeanFactory`, `ApplicationContext`
  - Bean管理：`BeanDefinition`封装，支持字段注入、构造器注入
  - 生命周期：`@PostConstruct`, `@PreDestroy`注解，完整生命周期管理
  - 循环依赖：三级缓存机制解决循环依赖问题

- **AOP实现**
  - 动态代理：JDK动态代理 + CGLIB代理，自动选择代理方式
  - 切面注解：`@Aspect`, `@Before`, `@After`, `@Around`, `@Pointcut`
  - 切点表达式：支持类和方法匹配
  - 代理工厂：`ProxyFactory`自动生成代理对象

**面试考点**：
- Bean生命周期流程
- 循环依赖解决方案（三级缓存）
- JDK动态代理 vs CGLIB代理
- AOP底层原理

**代码文件**：
- [mini-spring-core/src/main/java/](mini-spring-core/src/main/java/com/bootcloud/core/)

#### mini-spring-boot
- **自动配置**
  - 条件注解：`@ConditionalOnClass`, `@ConditionalOnProperty`
  - 自动配置加载：`spring.factories`机制
  - 条件评估：`OnClassCondition`, `OnPropertyCondition`

- **嵌入式容器**
  - Netty服务器：基于Netty的HTTP服务器实现
  - 请求映射：`@RequestMapping`, `@GetMapping`, `@PostMapping`
  - 控制器：`@RestController`注解
  - 请求处理：`HttpRequestHandler`处理HTTP请求

**面试考点**：
- Spring Boot自动配置原理
- spring.factories机制
- 嵌入式容器原理
- HTTP请求处理流程

**代码文件**：
- [mini-spring-boot/src/main/java/](mini-spring-boot/src/main/java/com/bootcloud/boot/)

### ✅ 性能优化层（100%）

#### jvm-optimizer
- **JVM信息监控**
  - 内存信息：堆内存、非堆内存、使用率
  - 线程信息：线程数、峰值线程数
  - 类信息：加载类数、总加载类数
  - GC信息：GC次数、GC时间
  - 运行时信息：JVM版本、运行时间

- **GC调优**
  - GC统计：收集GC收集信息
  - 调优建议：根据GC情况提供调优建议
  - 算法检测：检测当前使用的GC算法

- **内存管理**
  - 内存泄漏模拟：模拟OOM场景
  - 正常使用模拟：模拟正常内存使用
  - JVM性能分析：实时监控JVM性能

**面试考点**：
- JVM内存模型
- G1GC/ZGC调优
- OOM排查方法
- GC算法原理

**代码文件**：
- [jvm-optimizer/src/main/java/](jvm-optimizer/src/main/java/com/bootcloud/jvm/)

#### concurrent-optimizer
- **智能线程池**
  - CPU密集型线程池：根据CPU核心数配置
  - IO密集型线程池：根据IO特性配置
  - 动态调参：运行时调整线程池参数
  - 统计监控：任务执行统计、性能分析

- **锁性能对比**
  - synchronized锁：基础同步锁
  - ReentrantLock：可重入锁
  - ReadWriteLock：读写锁
  - AtomicInteger：CAS原子操作
  - 性能对比：各锁性能对比分析

- **死锁检测**
  - 死锁检测：自动检测死锁
  - 线程转储：生成线程转储信息
  - 死锁预防：提供死锁预防建议

- **并发工具**
  - CountDownLatch：倒计时门栓
  - CyclicBarrier：循环栅栏
  - Semaphore：信号量
  - BlockingQueue：阻塞队列

**面试考点**：
- 线程池原理和配置
- synchronized锁升级过程
- CAS原理和ABA问题
- 死锁检测和避免

**代码文件**：
- [concurrent-optimizer/src/main/java/](concurrent-optimizer/src/main/java/com/bootcloud/concurrent/)

### ✅ 文档（100%）

#### 架构设计文档
- [ARCHITECTURE.md](ARCHITECTURE.md)
- 架构分层图
- 模块依赖图
- 核心流程图
- 核心接口设计
- 配置文件设计
- 面试考点总结

#### 面试问答清单
- [INTERVIEW_QUESTIONS.md](INTERVIEW_QUESTIONS.md)
- 20道高频面试题
- 每题包含：问题、标准应答、代码示例、面试要点
- 涵盖：IOC、AOP、Spring Boot、微服务、JVM、并发

#### 快速启动手册
- [QUICKSTART.md](QUICKSTART.md)
- 环境准备
- 项目构建
- 运行示例
- 面试准备
- 常见问题
- 进阶使用
- 性能调优

#### 项目README
- [README.md](README.md)
- 项目简介
- 快速开始
- 核心功能
- 面试准备
- 性能优化

## 待完成模块

### ✅ 微服务组件层（80%）

#### mini-spring-cloud-registry（已完成）
- **服务注册中心实现**：基于Netty的RESTful API
- **服务注册与发现**：支持服务实例注册、注销、查询
- **心跳机制**：服务实例定期发送心跳维持活跃状态
- **健康检查**：定期检查服务实例健康状态，剔除不可用实例

**核心接口**：
- `ServiceRegistry`：服务注册接口
- `ServiceDiscovery`：服务发现接口
- `ServiceInstance`：服务实例模型

**实现类**：
- `InMemoryServiceRegistry`：基于内存的服务注册中心
- `DefaultServiceDiscovery`：默认服务发现实现，支持本地缓存
- `RegistryServer`：基于Netty的注册中心服务器

#### mini-spring-cloud-feign（待实现）
- 远程服务调用
- 声明式RPC
- HTTP/RPC通信
- 序列化支持

**计划实现**：
- `@FeignClient`注解
- 动态代理生成
- HTTP客户端
- JSON/Protobuf序列化

#### mini-spring-cloud-loadbalancer（已完成）
- **客户端负载均衡**：在客户端选择目标服务实例
- **多种负载均衡策略**：支持4种常用策略
- **权重支持**：支持通过元数据配置实例权重

**核心接口**：
- `LoadBalancer`：负载均衡接口
- `LoadBalancerFactory`：负载均衡器工厂

**实现类**：
- `RoundRobinLoadBalancer`：轮询策略
- `RandomLoadBalancer`：随机策略
- `WeightedRoundRobinLoadBalancer`：加权轮询策略
- `LeastActiveLoadBalancer`：最少活跃数策略

#### mini-spring-cloud-circuitbreaker（已完成）
- **服务熔断与降级**：防止服务雪崩，提供降级方案
- **状态机管理**：CLOSED、OPEN、HALF_OPEN三种状态
- **降级策略**：支持配置降级方法

**核心接口**：
- `CircuitBreaker`：熔断器接口
- `CircuitBreakerConfig`：熔断器配置
- `CircuitBreakerState`：熔断器状态枚举

**实现类**：
- `DefaultCircuitBreaker`：默认熔断器实现
- `CircuitBreakerFactory`：熔断器工厂
- `CircuitBreakerOpenException`：熔断打开异常

#### mini-spring-gateway（已完成）
- **API网关**：作为微服务的统一入口
- **路由转发**：根据请求路径转发到对应微服务
- **请求过滤**：支持过滤器链处理请求
- **限流**：基于客户端ID的限流功能

**核心接口**：
- `Gateway`：网关接口
- `GatewayFilter`：过滤器接口
- `GatewayFilterChain`：过滤器链接口

**实现类**：
- `DefaultGateway`：默认网关实现
- `LoggingFilter`：日志过滤器
- `RateLimitFilter`：限流过滤器

### ✅ 示例应用层（100%）

#### demo-app（已完成）
- **user-service**：用户服务示例
  - 用户CRUD操作
  - 用户数据存储
  - 用户查询接口

- **order-service**：订单服务示例
  - 订单CRUD操作
  - 订单数据存储
  - 订单查询接口
  - 集成熔断器

- **goods-service**：商品服务示例
  - 商品CRUD操作
  - 商品数据存储
  - 商品查询接口
  - 库存管理

**技术特点**：
- 完整的微服务架构
- 模块化设计
- 依赖注入
- 注解驱动
- 数据隔离

### ⏳ 测试与优化（部分完成）

#### 单元测试
- 已完成：mini-spring-core、jvm-optimizer、concurrent-optimizer的单元测试
- 待完成：其他模块的单元测试
- 目标：覆盖率≥80%

#### 性能压测
- 端到端集成测试
- 性能压测
- 压测数据收集

#### 性能调优报告
- JVM调优前后对比
- 并发优化前后对比
- 可视化报告

## 项目特色

### 1. 面试导向
- 每个模块都绑定面试考点
- 核心代码添加详细注释
- 提供标准面试应答

### 2. 从零实现
- 不依赖Spring框架
- 手写所有核心逻辑
- 深度理解底层原理

### 3. 代码质量
- 模块化设计
- 接口优先
- 完整单元测试
- 详细文档

### 4. 实用性
- 可编译、可运行
- 提供完整示例
- 快速启动手册
- 性能优化建议

## 技术亮点

### IOC容器
- 三级缓存解决循环依赖
- 完整的Bean生命周期
- 支持多种依赖注入方式

### AOP实现
- JDK动态代理 + CGLIB代理
- 自动选择代理方式
- 支持多种通知类型

### Spring Boot
- 自动配置机制
- 条件化加载
- 嵌入式Netty服务器

### JVM优化
- 实时监控JVM状态
- GC调优建议
- OOM排查指导

### 并发优化
- 智能线程池
- 锁性能对比
- 死锁检测
- 并发工具封装

## 学习路径建议

### 初学者路径
1. 阅读[ARCHITECTURE.md](ARCHITECTURE.md)了解整体架构
2. 运行IOC容器示例，理解Bean生命周期
3. 运行AOP示例，理解动态代理原理
4. 阅读[INTERVIEW_QUESTIONS.md](INTERVIEW_QUESTIONS.md)相关章节

### 进阶路径
1. 深入研究Spring Boot自动配置原理
2. 运行JVM监控示例，理解JVM内存模型
3. 运行并发优化示例，理解线程池和锁机制
4. 结合代码，准备面试应答

### 高级路径
1. 研究每个模块的源码实现
2. 扩展功能，添加新特性
3. 进行性能压测，优化性能
4. 准备复杂面试场景

## 面试准备建议

### 重点掌握
1. **IOC容器**：Bean生命周期、循环依赖、依赖注入
2. **AOP**：动态代理、切面原理、通知顺序
3. **Spring Boot**：自动配置、启动流程、嵌入式容器
4. **JVM**：内存模型、GC算法、性能调优
5. **并发**：线程池、锁机制、CAS、死锁

### 实践建议
1. 运行所有单元测试，理解功能
2. 修改代码，观察变化
3. 结合代码，准备面试应答
4. 模拟面试，提高表达

### 资料推荐
1. 本项目代码和文档
2. Spring官方文档
3. JVM调优实战书籍
4. 并发编程实战书籍

## 项目价值

### 面试价值
- 深度理解Spring生态原理
- 掌握JVM调优方法
- 理解并发编程要点
- 具备项目实战经验

### 学习价值
- 从零理解框架设计
- 掌握核心设计模式
- 提升编码能力
- 培养架构思维

### 实践价值
- 可直接用于面试展示
- 可作为项目经验
- 可扩展生产使用
- 可作为学习模板

## 后续计划

### 短期计划
1. 完成微服务组件层开发
2. 完成示例应用开发
3. 提升单元测试覆盖率
4. 进行性能压测

### 中期计划
1. 完善文档和注释
2. 添加更多功能特性
3. 优化性能和稳定性
4. 发布稳定版本

### 长期计划
1. 支持更多微服务特性
2. 集成更多监控工具
3. 提供生产级支持
4. 建立社区生态

## 贡献指南

欢迎贡献代码和建议：

1. Fork本仓库
2. 创建特性分支
3. 提交代码
4. 推送到分支
5. 创建Pull Request

## 联系方式

- 项目地址：[GitHub Repository]
- 问题反馈：[Issue Tracker]
- 技术交流：[Discussion Forum]

---

**项目完成度：核心模块100%，微服务模块待开发**

**祝面试成功，Offer多多！** 🚀
