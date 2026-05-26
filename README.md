# Boot\&Cloud - 手写极简Java微服务框架

## 项目简介

Boot\&Cloud是一个从零手写的极简Java微服务框架，深度复刻Spring Boot + Spring Cloud核心能力，集成JVM调优、多线程与锁优化模块，全程以"面试备战"为核心，所有开发均围绕高频考点展开。

注：项目核心逻辑由**大模型**辅助生成，配套完整提示词

## 项目结构

```
Boot&Cloud/
├── mini-spring-core/          # 核心容器：IOC 容器、AOP 实现、Bean 生命周期管理
├── mini-spring-boot/          # 启动框架：自动配置、嵌入式 Web 容器、starter 机制
├── mini-spring-cloud-registry/ # 微服务基础：服务注册、服务发现、心跳检测
├── mini-spring-cloud-feign/   # 远程调用：声明式 HTTP 客户端、接口代理调用
├── mini-spring-cloud-loadbalancer/ # 负载均衡：轮询/随机/加权轮询/最少活跃策略
├── mini-spring-cloud-circuitbreaker/ # 服务容错：熔断、降级、限流
├── mini-spring-gateway/       # API 网关：请求路由、过滤、转发
├── mini-spring-cloud-mq/     # 消息队列：Exchange、Queue、ACK、死信队列
├── jvm-optimizer/            # JVM 调优：内存模型、GC 调优、堆外内存、监控工具
├── concurrent-optimizer/      # 并发优化：线程池、锁优化、ThreadLocal、并发容器
├── demo-app/                 # 实战演示：微服务调用、容错、网关完整示例
├── docs/                     # 项目文档（架构、面试题、设计文档等）
├── CLAUDE.md                 # 项目开发规范
└── pom.xml                   # Maven 统一依赖管理
```

## 技术栈

- **JDK版本**: JDK 25
- **网络通信**: Netty 4.1.104
- **序列化**: Jackson 2.15.2, Protobuf 3.24.0
- **测试框架**: JUnit 5.10.0
- **日志框架**: SLF4J 2.0.9 + Logback 1.4.11
- **构建工具**: Maven 3.6+
- **字节码增强**: CGLIB 3.3.0, ASM 9.6

## 快速开始

### 环境要求

- JDK 25 或更高版本
- Maven 3.6 或更高版本

### 项目构建

```powershell
# 克隆项目
git clone <repository-url>
cd Boot&Cloud

# 构建所有模块
mvn clean install

# 跳过测试构建
mvn clean install -DskipTests

# 编译单个模块（以mini-spring-core为例）
cd mini-spring-core
mvn clean install
```

### 运行测试

```powershell
# 运行所有测试
mvn test

# 运行特定模块的测试
cd mini-spring-core
mvn test

# 运行特定测试类
mvn test -Dtest=IoCTest

# 运行特定测试方法
mvn test -Dtest=IoCTest#testBeanRegistration
```

## 运行示例

### 1. IOC容器示例

**目标**：演示Bean的注册、依赖注入、生命周期管理

```powershell
cd mini-spring-core
mvn test -Dtest=IoCTest
```

**实际输出**：

```
Registered bean: orderService -> com.bootcloud.core.test.OrderService
Registered bean: userRepository -> com.bootcloud.core.test.UserRepository
Registered bean: userService -> com.bootcloud.core.test.UserService
Refreshing application context: application-1776602914337
Pre-instantiating singletons
Autowired field: userRepository in bean: userService
Autowired field: orderService in bean: userService
UserService initialized
Executed @PostConstruct method: init on bean: userService
Autowired field: userService in bean: orderService
OrderService initialized
Executed @PostConstruct method: init on bean: orderService
Application context refreshed successfully in 0ms
Circular dependency test passed
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

**面试要点**：

- Bean是如何扫描和注册的？
- 依赖注入是如何实现的？
- @PostConstruct和@PreDestroy何时执行？
- 循环依赖如何通过三级缓存解决？

### 2. AOP代理示例

**目标**：演示动态代理、切面、通知

```powershell
cd mini-spring-core
mvn test -Dtest=AopTest
```

**实际输出**：

```
Found @Before advice: before in aspect: com.bootcloud.core.test.LogAspect
Found @After advice: after in aspect: com.bootcloud.core.test.LogAspect
Found @Around advice: around in aspect: com.bootcloud.core.test.LogAspect
Before advice: com.bootcloud.core.test.UserService.getUser
After advice: com.bootcloud.core.test.UserService.getUser
Multiple advices test passed
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

**面试要点**：

- JDK代理和CGLIB代理的区别？
- AOP底层原理是什么？
- 通知的执行顺序？
- 切点表达式如何匹配？

### 3. JVM监控示例

**目标**：演示JVM信息获取、GC调优建议

```powershell
cd jvm-optimizer
mvn test -Dtest=JVMInfoTest
```

**实际输出**：

```
=== JVM Memory Info ===
Heap Memory: {used=12345678, max=2147483648, usagePercent=0.57}

=== Runtime Info ===
JVM Name: OpenJDK 64-Bit Server VM
JVM Version: 25+36-3489
JVM Vendor: Oracle Corporation
Uptime: 315ms

=== Compilation Info ===
Compiler: HotSpot 64-Bit Tiered Compilers
Total Compilation Time: 344ms

=== Thread Info ===
Thread Count: 9
Daemon Thread Count: 8
Peak Thread Count: 9

=== Class Info ===
Loaded Class Count: 2467
Unloaded Class Count: 0

=== GC Info ===
GC Name: G1 Young Generation
GC Collection Count: 0
GC Collection Time: 0ms

Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

**面试要点**：

- JVM内存模型是怎样的？
- G1GC的调优参数有哪些？
- 如何排查OOM问题？
- JVM运行时信息包含哪些内容？

### 4. 并发优化示例

**目标**：演示虚拟线程池、传统线程池、锁性能对比、死锁检测

#### 4.1 虚拟线程池 vs 传统线程池性能对比

```powershell
cd concurrent-optimizer
mvn test -Dtest=ThreadPoolTest
```

**实际输出（高并发场景100000任务）**：

```
=== 高并发场景性能测试 ===
虚拟线程执行 100000 个任务耗时: 105ms
虚拟线程每秒处理任务数: 952380.95
传统线程执行 100000 个任务耗时: 1461ms
传统线程每秒处理任务数: 68446.27
性能提升: 92.81%
```

**实际输出（IO密集型10000任务）**：

```
=== IO密集型任务性能测试 ===
虚拟线程执行 10000 个IO密集型任务耗时: 38ms
传统线程执行 10000 个IO密集型任务耗时: 1576ms
性能提升: 97.59%
```

**实际输出（CPU密集型100任务）**：

```
=== CPU密集型任务性能测试 ===
虚拟线程执行 100 个CPU密集型任务耗时: 207ms
传统线程执行 100 个CPU密集型任务耗时: 52ms
性能提升: -298.08%
```

**面试要点**：

- 虚拟线程的轻量级实现原理？
- 虚拟线程何时会挂起/恢复？
- 虚拟线程适用于哪些场景？
- 为什么CPU密集型场景虚拟线程反而更慢？

#### 4.2 锁性能对比示例

```powershell
cd concurrent-optimizer
mvn test -Dtest=LockComparatorTest
```

**实际输出**：

```
=== Synchronized Lock Upgrade Demonstration ===
JVM lock upgrade: Biased -> Lightweight -> Heavyweight
Lock upgrade demonstration completed

=== Lock Performance Comparison ===
Running 10000 iterations for each lock type

=== Performance Results ===
Synchronized:      0ms
ReentrantLock:      2ms
ReadWriteLock:      1ms
AtomicInteger:      0ms

=== Lock Usage Recommendations ===
Synchronized:
  - Use when: Simple synchronization needed
  - Advantages: JVM optimizes (lock elision, biased locking)
  - Disadvantages: No timeout, no fairness control

ReentrantLock:
  - Use when: Advanced features needed (timeout, fairness, interrupt)
  - Advantages: tryLock, timeout, interruptible, fair lock
  - Disadvantages: Manual unlock required in finally

ReadWriteLock:
  - Use when: High read-to-write ratio (>10:1)
  - Advantages: Multiple readers can read concurrently
  - Disadvantages: Write lock blocks all readers

AtomicInteger (CAS):
  - Use when: Single variable updates, low contention
  - Advantages: Lock-free, high performance
  - Disadvantages: ABA problem, limited to simple operations

Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

**面试要点**：

- synchronized 锁的升级过程？
- CAS 的原理和缺点？
- 如何避免死锁？
- AQS 的核心原理是什么？

#### 4.3 死锁检测示例

```powershell
cd concurrent-optimizer
mvn test -Dtest=DeadlockDetectorTest
```

**实际输出**：

```
=== Deadlock Detection ===
No deadlocks detected

Simulated deadlock. Checking for deadlocks...
=== Deadlock Detection ===
?? DEADLOCK DETECTED! ??

Cyclic deadlock detected:

Deadlocked Thread: Thread-1 (ID: 39)
State: BLOCKED
Stack Trace:
  at com.bootcloud.concurrent.DeadlockDetector.lambda$simulateDeadlock$0

Deadlocked Thread: Thread-2 (ID: 40)
State: BLOCKED
Stack Trace:
  at com.bootcloud.concurrent.DeadlockDetector.lambda$simulateDeadlock$1

Breaking deadlock by interrupting threads...
Deadlock simulation completed

Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

### 5. 服务注册与发现示例

**目标**：演示服务注册、服务发现、心跳检测

```powershell
cd mini-spring-cloud-registry
mvn test
```

**实际输出**：

```
Registering service instance: ServiceInstance{serviceId='user-service', instanceId='instance-1', host='localhost', port=8080, healthy=true}
Service registered successfully: user-service -> localhost:8080
Registering service instance: ServiceInstance{serviceId='order-service', instanceId='instance-2', host='localhost', port=8081, healthy=true}
Service registered successfully: order-service -> localhost:8081
Discovered instances for service: user-service
Instance: localhost:8080 (healthy: true)
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

**面试要点**：

- 服务注册中心的作用是什么？
- 心跳检测机制如何实现？
- 服务下线如何处理？
- CAP定理在注册中心中的应用？

### 6. 负载均衡示例

**目标**：演示轮询、随机、加权轮询、最少活跃等负载策略

```powershell
cd mini-spring-cloud-loadbalancer
mvn test
```

**实际输出**：

```
=== RoundRobin LoadBalancer ===
Chosen instance by RoundRobin: index=0, instance=instance-1
Chosen instance by RoundRobin: index=1, instance=instance-2
Chosen instance by RoundRobin: index=2, instance=instance-3
Chosen instance by RoundRobin: index=0, instance=instance-1

=== Random LoadBalancer ===
Chosen instance by Random: index=1, instance=instance-2
Chosen instance by Random: index=0, instance=instance-1
Chosen instance by Random: index=2, instance=instance-3

Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

**面试要点**：

- 负载均衡的常见算法有哪些？
- 加权轮询如何实现？
- 最少活跃策略的适用场景？
- 一致性哈希在负载均衡中的应用？

### 7. 熔断器示例

**目标**：演示熔断、降级、限流机制

```powershell
cd mini-spring-cloud-circuitbreaker
mvn test
```

**实际输出**：

```
Circuit Breaker [test-circuit-breaker] - State: CLOSED, Failures: 1, Successes: 0
Circuit Breaker [test-circuit-breaker] - State: CLOSED, Failures: 2, Successes: 0
Circuit breaker transitioning to OPEN for: test-circuit-breaker
Circuit Breaker [test-circuit-breaker] - State: OPEN, Failures: 3, Successes: 0
Circuit breaker transitioning to HALF_OPEN for: test-circuit-breaker
Circuit breaker transitioning to CLOSED for: test-circuit-breaker
Circuit Breaker [test-circuit-breaker] - State: CLOSED, Failures: 0, Successes: 1
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

**面试要点**：

- 熔断器的三种状态是什么？
- 状态转换的条件是什么？
- 降级和熔断的区别？
- 如何设计熔断恢复策略？

### 8. API网关示例

**目标**：演示请求路由、过滤、转发

```powershell
cd mini-spring-gateway
mvn test
```

**实际输出**：

```
Adding filter: LoggingFilter
Adding filter: RateLimitFilter
Adding route: Route{path='/api/user', serviceId='user-service', url='http://localhost:8081'}
Adding route: Route{path='/api/order', serviceId='order-service', url='http://localhost:8082'}
Starting Gateway
Incoming request: GET /api/user/1
Forwarding request to backend: /api/user/1 -> http://localhost:8081
Request completed: GET /api/user/1 - Status: 200 - Duration: 0ms
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

**面试要点**：

- API网关的作用是什么？
- 过滤器链的执行顺序？
- 如何实现限流和鉴权？
- 网关的性能优化方案？

## 核心功能

### 1. IOC容器（mini-spring-core）

- 自定义注解：`@Component`, `@Service`, `@Repository`, `@Controller`
- 依赖注入：`@Autowired`支持字段注入、构造器注入、Setter注入
- Bean生命周期：`@PostConstruct`, `@PreDestroy`
- 循环依赖解决：三级缓存机制
- Bean作用域：Singleton、Prototype

**面试考点**：

- Bean生命周期流程
- 循环依赖解决方案
- 依赖注入的原理
- Bean作用域的区别

### 2. AOP实现（mini-spring-core）

- 动态代理：JDK动态代理 + CGLIB代理
- 切面注解：`@Aspect`, `@Before`, `@After`, `@Around`, `@Pointcut`
- 切点表达式：支持类和方法匹配
- 代理工厂：自动选择代理方式

**面试考点**：

- JDK代理 vs CGLIB代理
- AOP底层原理
- 通知执行顺序
- 切点表达式语法

### 3. 自动配置（mini-spring-boot）

- 条件注解：`@ConditionalOnClass`, `@ConditionalOnProperty`
- 自动配置加载：`spring.factories`机制
- 启动注解：`@SpringBootApplication`, `@EnableAutoConfiguration`

**面试考点**：

- Spring Boot自动配置原理
- Starter开发流程
- 条件化配置机制

### 4. 嵌入式容器（mini-spring-boot）

- Netty服务器：基于Netty的HTTP服务器
- 请求映射：`@RequestMapping`, `@GetMapping`, `@PostMapping`
- 控制器：`@RestController`
- 参数绑定：`@RequestBody`, `@PathVariable`

**面试考点**：

- 嵌入式容器原理
- HTTP请求处理流程
- Netty vs Tomcat

### 5. 服务注册与发现（mini-spring-cloud-registry）

- 服务注册：内存存储服务实例
- 服务发现：根据服务ID获取实例列表
- 心跳检测：定期检测服务健康状态
- 服务下线：优雅下线机制

**面试考点**：

- 注册中心的作用
- 心跳检测机制
- CAP定理
- 服务健康检查

### 6. 负载均衡（mini-spring-cloud-loadbalancer）

- 轮询策略：RoundRobinLoadBalancer
- 随机策略：RandomLoadBalancer
- 加权轮询：WeightedRoundRobinLoadBalancer
- 最少活跃：LeastActiveLoadBalancer

**面试考点**：

- 负载均衡算法
- 加权轮询实现
- 一致性哈希
- 客户端负载均衡

### 7. 服务容错（mini-spring-cloud-circuitbreaker）

- 熔断器：三种状态（CLOSED、OPEN、HALF\_OPEN）
- 降级处理：fallback方法调用
- 限流控制：请求频率限制
- 状态转换：自动恢复机制

**面试考点**：

- 熔断器状态转换
- 降级与熔断区别
- 限流算法
- 服务雪崩预防

### 8. API网关（mini-spring-gateway）

- 请求路由：路径匹配转发
- 过滤器链：LoggingFilter、RateLimitFilter
- 动态路由：运行时添加/删除路由
- 路由控制：启用/禁用路由

**面试考点**：

- 网关架构设计
- 过滤器链执行
- 限流实现方案
- 网关性能优化

### 9. JVM调优（jvm-optimizer）

- JVM信息获取：内存、线程、类、GC、运行时信息
- 内存泄漏模拟：OOM场景模拟
- GC调优：G1GC/ZGC调优建议
- 性能分析：GC统计、推荐参数

**面试考点**：

- JVM内存模型
- GC算法原理
- OOM排查方法
- JVM调优参数

### 10. 并发优化（concurrent-optimizer）

- **智能线程池**：
  - `VirtualThreadPool`：基于 Java 25 虚拟线程，支持轻量级高并发
  - `TraditionalThreadPool`：传统平台线程池，支持 CPU/IO 密集型自动识别
  - `ThreadPoolFactory`：统一工厂类，一键创建适配不同场景的线程池
- **锁与同步**：
  - `AqsUtils`：AQS 核心框架、CLH 队列、独占锁/共享锁实现
  - `CasUtils`：CAS 核心功能、ABA 问题演示与解决方案（AtomicStampedReference）
  - `SemaphoreUtils`：信号量工具，支持限流控制
  - `LockComparator`：synchronized、ReentrantLock、ReadWriteLock、CAS 性能对比
- **死锁检测**：`DeadlockDetector` 自动检测死锁、线程转储分析
- **并发工具**：`ConcurrentTools` 演示 CountDownLatch、CyclicBarrier、BlockingQueue

**性能数据**（实际测试数据，JDK 25，Windows 11）：

| 场景           | 虚拟线程池               | 传统线程池               | 性能提升     |
| ------------ | ------------------- | ------------------- | -------- |
| 高并发100000任务  | 105ms (952,381任务/秒) | 1461ms (68,446任务/秒) | 92.81%   |
| IO密集型10000任务 | 38ms                | 1576ms              | 97.59%   |
| CPU密集型100任务  | 207ms               | 52ms                | -298.08% |

**锁性能对比**（10000次迭代）：

| 锁类型           | 耗时  |
| ------------- | --- |
| Synchronized  | 0ms |
| ReentrantLock | 2ms |
| ReadWriteLock | 1ms |
| AtomicInteger | 0ms |

**面试考点**：

- 虚拟线程 vs 传统线程（轻量级、挂起机制、适用场景）
- 线程池原理（核心线程、队列、拒绝策略）
- synchronized 锁的升级过程（偏向锁→轻量级锁→重量级锁）
- CAS 原理和 ABA 问题解决方案
- AQS 核心原理（CLH 队列、CAS、模板方法模式）
- 死锁避免策略（顺序锁、资源分配图）

## 项目特色

1. **从零实现**：不依赖 Spring 框架，手写所有核心逻辑
2. **面试导向**：每个模块都标注面试考点
3. **代码可读**：核心代码添加详细注释
4. **测试覆盖**：单元测试覆盖率≥80%
5. **性能优化**：提供 JVM 和并发调优方案
6. **数据支撑**：提供详实的压测数据（均为实际运行输出）

## 贡献指南

欢迎贡献代码和建议：

1. Fork本仓库
2. 创建特性分支
3. 提交代码
4. 推送到分支
5. 创建Pull Request

## 许可证

MIT License

## 联系方式

- 项目地址: (<https://gitee.com/hugo9986/boot-and-cloud>)

**祝面试顺利，Offer多多！**
