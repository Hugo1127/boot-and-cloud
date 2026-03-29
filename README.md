# Boot&Cloud - 手写极简Java微服务框架

## 项目简介

Boot&Cloud是一个从零手写的极简Java微服务框架，深度复刻Spring Boot + Spring Cloud核心能力，集成JVM调优、多线程与锁优化模块，全程以"面试备战"为核心，所有开发均围绕高频考点展开。

## 核心目标

- **代码落地**：从零实现核心功能，手写所有关键逻辑
- **原理吃透**：深度理解底层实现机制（反射、动态代理、类加载等）
- **面试应答**：每个模块绑定面试考点，提供标准问答

## 项目结构

```
Boot&Cloud/
├── mini-spring-core/          # IOC、AOP核心实现
├── mini-spring-boot/          # 自动配置、嵌入式容器
├── mini-spring-cloud-registry/ # 服务注册与发现
├── mini-spring-cloud-feign/   # 远程服务调用
├── mini-spring-cloud-loadbalancer/ # 客户端负载均衡
├── mini-spring-cloud-circuitbreaker/ # 服务熔断与降级
├── mini-spring-gateway/       # API网关
├── jvm-optimizer/            # JVM调优与监控
├── concurrent-optimizer/      # 多线程与锁优化
├── demo-app/                 # 示例微服务应用
├── ARCHITECTURE.md           # 架构设计文档
├── INTERVIEW_QUESTIONS.md    # 面试问答清单
└── pom.xml                   # Maven构建配置
```

## 技术栈

- **JDK版本**: JDK 17+
- **网络通信**: Netty
- **序列化**: Jackson, Protobuf
- **测试框架**: JUnit 5
- **日志框架**: SLF4J + Logback
- **构建工具**: Maven

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- IDE：IntelliJ IDEA（推荐）

### 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd Boot&Cloud

# 构建所有模块
mvn clean install

# 运行单元测试
mvn test
```

### 运行示例

```bash
# 运行IOC容器示例
cd mini-spring-core
mvn test -Dtest=IoCTest

# 运行AOP示例
mvn test -Dtest=AopTest

# 运行JVM监控示例
cd jvm-optimizer
mvn exec:java -Dexec.mainClass="com.bootcloud.jvm.JVMInfo"

# 运行并发优化示例
cd concurrent-optimizer
mvn exec:java -Dexec.mainClass="com.bootcloud.concurrent.LockComparator"
```

## 核心功能

### 1. IOC容器（mini-spring-core）

- 自定义注解：`@Component`, `@Service`, `@Repository`, `@Controller`
- 依赖注入：`@Autowired`支持字段注入、构造器注入、Setter注入
- Bean生命周期：`@PostConstruct`, `@PreDestroy`
- 循环依赖解决：三级缓存机制

**面试考点**：
- Bean生命周期流程
- 循环依赖解决方案
- 依赖注入原理

### 2. AOP实现（mini-spring-core）

- 动态代理：JDK动态代理 + CGLIB代理
- 切面注解：`@Aspect`, `@Before`, `@After`, `@Around`, `@Pointcut`
- 切点表达式：支持类和方法匹配
- 代理工厂：自动选择代理方式

**面试考点**：
- JDK代理 vs CGLIB代理
- AOP底层原理
- 通知执行顺序

### 3. 自动配置（mini-spring-boot）

- 条件注解：`@ConditionalOnClass`, `@ConditionalOnProperty`
- 自动配置加载：`spring.factories`机制
- 启动注解：`@SpringBootApplication`, `@EnableAutoConfiguration`

**面试考点**：
- Spring Boot自动配置原理
- Starter开发流程
- 条件化配置

### 4. 嵌入式容器（mini-spring-boot）

- Netty服务器：基于Netty的HTTP服务器
- 请求映射：`@RequestMapping`, `@GetMapping`, `@PostMapping`
- 控制器：`@RestController`
- 参数绑定：`@RequestBody`, `@PathVariable`

**面试考点**：
- 嵌入式容器原理
- HTTP请求处理流程
- Netty vs Tomcat

### 5. JVM调优（jvm-optimizer）

- JVM信息获取：内存、线程、类、GC、运行时信息
- 内存泄漏模拟：OOM场景模拟
- GC调优：G1GC/ZGC调优建议
- 性能分析：GC统计、推荐参数

**面试考点**：
- JVM内存模型
- GC算法原理
- OOM排查方法

### 6. 并发优化（concurrent-optimizer）

- 智能线程池：支持动态调参、统计监控
- 锁性能对比：synchronized、ReentrantLock、ReadWriteLock、CAS
- 死锁检测：自动检测死锁、线程转储
- 并发工具：CountDownLatch、CyclicBarrier、Semaphore

**面试考点**：
- 线程池原理
- 锁优化机制
- CAS原理
- 死锁避免

## 面试准备

### 高频考点总结

本项目涵盖了Java后端面试的高频考点：

1. **Spring生态**：IOC、AOP、自动配置、启动流程
2. **微服务**：服务注册发现、负载均衡、熔断降级、RPC
3. **JVM调优**：内存模型、GC算法、性能调优、OOM排查
4. **并发编程**：线程池、锁优化、CAS、并发工具、死锁

### 面试技巧

1. **理论+实践**：结合项目代码讲解原理
2. **问题导向**：针对面试官的问题，引用项目中的实现
3. **对比分析**：对比不同方案，说明选型依据
4. **性能数据**：提供调优前后的性能对比数据

### 推荐学习顺序

1. 先理解架构设计（阅读ARCHITECTURE.md）
2. 逐个模块学习代码（mini-spring-core → mini-spring-boot → ...）
3. 运行单元测试，验证功能
4. 阅读面试问答（INTERVIEW_QUESTIONS.md）
5. 结合代码，准备面试应答

## 性能优化

### JVM调优建议

```bash
# G1GC调优参数
-Xms2g -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m
-XX:InitiatingHeapOccupancyPercent=45

# ZGC调优参数（JDK 11+）
-Xms2g -Xmx2g
-XX:+UnlockExperimentalVMOptions
-XX:+UseZGC
-XX:ZCollectionInterval=5
```

### 线程池配置

```java
// CPU密集型
SmartThreadPool cpuPool = SmartThreadPool.createCpuIntensivePool("cpu-pool");

// IO密集型
SmartThreadPool ioPool = SmartThreadPool.createIoIntensivePool("io-pool");

// 动态调参
pool.dynamicResize(newCoreSize, newMaxSize);
```

## 项目特色

1. **从零实现**：不依赖Spring框架，手写所有核心逻辑
2. **面试导向**：每个模块都标注面试考点
3. **代码可读**：核心代码添加详细注释
4. **测试覆盖**：单元测试覆盖率≥80%
5. **性能优化**：提供JVM和并发调优方案

## 贡献指南

欢迎贡献代码和建议：

1. Fork本仓库
2. 创建特性分支
3. 提交代码
4. 推送到分支
5. 创建Pull Request

## 文档

- [架构设计文档](ARCHITECTURE.md)
- [面试问答清单](INTERVIEW_QUESTIONS.md)
- [各模块README](各模块目录下)

## 许可证

MIT License

## 联系方式

- 项目地址：[GitHub Repository]
- 问题反馈：[Issue Tracker]
- 技术交流：[Discussion Forum]

---

**祝面试顺利，Offer多多！** 🚀
