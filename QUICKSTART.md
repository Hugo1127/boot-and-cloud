# Boot&Cloud 快速启动手册

## 目录

1. [环境准备](#环境准备)
2. [项目构建](#项目构建)
3. [运行示例](#运行示例)
4. [面试准备](#面试准备)
5. [常见问题](#常见问题)

---

## 环境准备

### 必需软件

| 软件 | 版本要求 | 下载地址 |
|------|---------|---------|
| JDK | 17+ | https://www.oracle.com/java/technologies/downloads/ |
| Maven | 3.6+ | https://maven.apache.org/download.cgi |
| IDE | IntelliJ IDEA（推荐） | https://www.jetbrains.com/idea/ |

### 环境变量配置

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

### IDE配置

1. 导入Maven项目：File → Open → 选择pom.xml
2. 配置Maven：File → Settings → Build, Execution, Deployment → Build Tools → Maven
3. 配置JDK：File → Project Structure → Project → SDK选择JDK 17

---

## 项目构建

### 克隆项目

```bash
git clone <repository-url>
cd Boot&Cloud
```

### 构建所有模块

```bash
# 清理并构建
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
```

### 查看测试覆盖率

```bash
# 运行测试并生成覆盖率报告
mvn clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

---

## 运行示例

### 1. IOC容器示例

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

---

### 2. AOP代理示例

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

---

### 3. JVM监控示例

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

---

### 4. 并发优化示例

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

---

### 5. 完整Web应用示例

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

---

## 面试准备

### 学习路径

1. **第一阶段：理解架构**
   - 阅读[ARCHITECTURE.md](ARCHITECTURE.md)
   - 理解项目分层和模块依赖
   - 掌握核心流程

2. **第二阶段：逐个模块学习**
   - IOC容器：重点理解Bean生命周期、循环依赖
   - AOP：重点理解动态代理、切面机制
   - Spring Boot：重点理解自动配置、启动流程
   - JVM：重点理解内存模型、GC算法
   - 并发：重点理解线程池、锁机制

3. **第三阶段：运行示例**
   - 运行所有单元测试
   - 查看测试覆盖率报告
   - 修改代码，观察变化

4. **第四阶段：准备面试**
   - 阅读[INTERVIEW_QUESTIONS.md](INTERVIEW_QUESTIONS.md)
   - 结合代码，准备应答
   - 模拟面试，提高表达

### 高频考点速查

| 考点 | 关键代码 | 面试题 |
|------|---------|--------|
| IOC | DefaultListableBeanFactory | Bean生命周期、循环依赖 |
| AOP | ProxyFactory | JDK vs CGLIB、AOP原理 |
| 自动配置 | AutoConfigurationLoader | spring.factories、条件化配置 |
| JVM | JVMInfo, GCTuner | 内存模型、GC调优、OOM排查 |
| 并发 | SmartThreadPool, LockComparator | 线程池参数、锁优化、CAS |

### 面试技巧

1. **结合代码讲解**：
   - "在我们项目中，IOC容器通过三级缓存解决循环依赖..."
   - 代码位置：[DefaultListableBeanFactory](mini-spring-core/src/main/java/com/bootcloud/core/factory/DefaultListableBeanFactory.java#L50-L70)

2. **对比分析**：
   - "JDK代理基于接口，CGLIB代理基于继承..."
   - "在我们的ProxyFactory中，根据目标类是否实现接口自动选择代理方式"

3. **性能数据**：
   - "从锁性能对比测试来看，AtomicInteger性能最好（12ms）..."
   - "G1GC的停顿时间目标可以设置为200ms..."

---

## 常见问题

### 1. Maven构建失败

**问题**：`mvn install` 失败

**解决方案**：
```bash
# 清理Maven缓存
rm -rf ~/.m2/repository

# 重新构建
mvn clean install -U

# 检查网络连接
ping repo.maven.org
```

### 2. JDK版本不匹配

**问题**：`Unsupported class file major version 61`

**解决方案**：
```bash
# 检查JDK版本
java -version

# 设置JAVA_HOME
export JAVA_HOME=/path/to/jdk-17

# 或在pom.xml中指定
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

### 3. 测试失败

**问题**：单元测试失败

**解决方案**：
```bash
# 单独运行失败的测试
mvn test -Dtest=FailedTestClass

# 查看详细错误信息
mvn test -X

# 跳过失败的测试
mvn test -Dmaven.test.failure.ignore=true
```

### 4. 端口被占用

**问题**：`Address already in use: 8080`

**解决方案**：
```bash
# Windows
netstat -ano | findstr :8080
taskkill /F /PID <PID>

# Linux/Mac
lsof -i :8080
kill -9 <PID>

# 或修改端口
# application.properties
server.port=8081
```

### 5. 内存溢出

**问题**：`java.lang.OutOfMemoryError: Java heap space`

**解决方案**：
```bash
# 增加堆内存
export MAVEN_OPTS="-Xmx2g -Xms2g"

# 或在pom.xml中配置
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Xmx2g -Xms2g</argLine>
    </configuration>
</plugin>
```

---

## 进阶使用

### 自定义Bean扫描

```java
@SpringBootApplication(scanBasePackages = {"com.example.service"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 自定义线程池

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

### JVM性能监控

```java
JVMProfiler profiler = new JVMProfiler(5000); // 5秒间隔
profiler.start();

// 运行一段时间后
Thread.sleep(30000);

profiler.stop();

// 获取GC调优建议
GCTuner.printGCRecommendations();
```

---

## 性能调优

### JVM调优参数

```bash
# G1GC推荐配置
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

---

## 联系与支持

- **文档**：[README.md](README.md), [ARCHITECTURE.md](ARCHITECTURE.md), [INTERVIEW_QUESTIONS.md](INTERVIEW_QUESTIONS.md)
- **问题反馈**：提交Issue到GitHub仓库
- **技术交流**：参与Discussion论坛

---

**祝你面试成功，Offer多多！** 🎉
