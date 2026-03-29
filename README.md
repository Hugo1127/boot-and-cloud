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

### 环境准备

#### 必需软件

| 软件 | 版本要求 | 下载地址 |
|------|---------|---------|
| JDK | 17+ | https://www.oracle.com/java/technologies/downloads/ |
| Maven | 3.6+ | https://maven.apache.org/download.cgi |
| IDE | IntelliJ IDEA（推荐） | https://www.jetbrains.com/idea/ |

#### 环境变量配置

```bash
# 设置JAVA_HOME
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# 设置MAVEN_HOME
export MAVEN_HOME=/path/to/maven
export PATH=$MAVEN_HOME/bin:$PATH

# 验证安装
java -version
mvn -version
```

#### IDE配置

1. 导入Maven项目：File → Open → 选择pom.xml
2. 配置Maven：File → Settings → Build, Execution, Deployment → Build Tools → Maven
3. 配置JDK：File → Project Structure → Project → SDK选择JDK 17

### 项目构建

```bash
# 克隆项目
git clone <repository-url>
cd Boot&Cloud

# 构建所有模块
mvn clean install

# 跳过测试构建
mvn clean install -DskipTests

# 编译单个模块
cd mini-spring-core
mvn clean install
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=IoCTest

# 运行特定测试方法
mvn test -Dtest=IoCTest#testBeanRegistration

# 运行测试并生成覆盖率报告
mvn clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

### 运行示例

#### 1. IOC容器示例
**目标**：演示Bean的注册、依赖注入、生命周期管理
```bash
cd mini-spring-core
mvn test -Dtest=IoCTest
```
**预期输出**：
```
INFO  - Registered bean: userRepository -> com.bootcloud.core.test.UserRepository
INFO  - Registered bean: userService -> com.bootcloud.core.test.UserService
INFO  - UserService initialized
INFO  - OrderService initialized
```
**面试要点**：
- Bean是如何扫描和注册的？
- 依赖注入是如何实现的？
- @PostConstruct和@PreDestroy何时执行？

#### 2. AOP代理示例
**目标**：演示动态代理、切面、通知
```bash
cd mini-spring-core
mvn test -Dtest=AopTest
```
**预期输出**：
```
INFO  - Found @Before advice: before in aspect: com.bootcloud.core.test.LogAspect
INFO  - Found @After advice: after in aspect: com.bootcloud.core.test.LogAspect
INFO  - Found @Around advice: around in aspect: com.bootcloud.core.test.LogAspect
INFO  - Before advice: com.bootcloud.core.test.UserService.getUser
INFO  - After advice: com.bootcloud.core.test.UserService.getUser
```
**面试要点**：
- JDK代理和CGLIB代理的区别？
- AOP底层原理是什么？
- 通知的执行顺序？

#### 3. JVM监控示例
**目标**：演示JVM信息获取、GC调优建议
```bash
cd jvm-optimizer
mvn test -Dtest=JVMInfoTest
```
**预期输出**：
```
=== JVM Memory Info ===
{heap.used=12345678, heap.max=2147483648, heap.usagePercent=0.57}

=== GC Info ===
{gcName=G1 Young Generation, gcCollectionCount=10, gcCollectionTime=123}
```
**面试要点**：
- JVM内存模型是怎样的？
- G1GC的调优参数有哪些？
- 如何排查OOM问题？

#### 4. 并发优化示例
**目标**：演示线程池、锁性能对比、死锁检测
```bash
cd concurrent-optimizer
mvn test -Dtest=LockComparatorTest
```
**预期输出**：
```
=== Lock Performance Comparison ===
Running 10000 iterations for each lock type

Synchronized:      15ms
ReentrantLock:      18ms
ReadWriteLock:      22ms
AtomicInteger:      12ms

=== Lock Usage Recommendations ===
...
```
**面试要点**：
- synchronized锁的升级过程？
- CAS的原理和缺点？
- 如何避免死锁？

#### 5. 完整Web应用示例
**目标**：演示Spring Boot风格的Web应用
```bash
# 创建启动类（在demo-app中）
# 运行启动类
cd demo-app
mvn exec:java -Dexec.mainClass="com.bootcloud.demo.Application"
```
**访问应用**：
```
http://localhost:8080/api/hello
http://localhost:8080/api/user/1
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

# G1GC推荐配置（完整启动参数）
java -Xms2g -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:G1HeapRegionSize=16m \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/tmp/heapdump.hprof \
     -jar app.jar
```

### 线程池配置
```java
// CPU密集型
SmartThreadPool cpuPool = SmartThreadPool.createCpuIntensivePool("cpu-pool");

// IO密集型
SmartThreadPool ioPool = SmartThreadPool.createIoIntensivePool("io-pool");

// 动态调参
pool.dynamicResize(newCoreSize, newMaxSize);

// CPU密集型（计算密集）
int coreSize = Runtime.getRuntime().availableProcessors() + 1;
int maxSize = Runtime.getRuntime().availableProcessors() + 1;

// IO密集型（网络、数据库）
int coreSize = Runtime.getRuntime().availableProcessors() * 2;
int maxSize = Runtime.getRuntime().availableProcessors() * 4;

// 混合型
int coreSize = Runtime.getRuntime().availableProcessors();
int maxSize = Runtime.getRuntime().availableProcessors() * 2;
```

### 进阶使用示例
#### 自定义Bean扫描
```java
@SpringBootApplication(scanBasePackages = {"com.example.service"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### 自定义线程池
```java
SmartThreadPool pool = new SmartThreadPool(
    8,                              // corePoolSize
    16,                             // maximumPoolSize
    60L, TimeUnit.SECONDS,            // keepAliveTime
    new LinkedBlockingQueue<>(100),     // workQueue
    "custom-pool"                    // poolName
);

// 动态调参
pool.dynamicResize(10, 20);

// 查看统计
pool.printStatistics();
```

#### JVM性能监控
```java
JVMProfiler profiler = new JVMProfiler(5000); // 5秒间隔
profiler.start();

// 运行一段时间后
Thread.sleep(30000);

profiler.stop();

// 获取GC调优建议
GCTuner.printGCRecommendations();
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

## 许可证
MIT License

## 联系方式
- 项目地址：[GitHub Repository]

**祝面试顺利，Offer多多！** 🚀
