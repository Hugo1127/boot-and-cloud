# Boot&Cloud 面试问答清单

## 一、IOC容器面试题

### Q1: 什么是IoC（控制反转）？什么是DI（依赖注入）？二者有什么区别？

**应答：**
- **IoC（控制反转）**：是一种设计思想，将对象的创建和管理权交给容器，而不是由对象自己管理。传统的对象创建由程序直接控制，IoC则将控制权反转给容器。
- **DI（依赖注入）**：是IoC的具体实现方式，容器在创建对象时，自动将依赖的对象注入到目标对象中。
- **区别**：IoC是思想，DI是实现方式。IoC强调控制权的转移，DI强调依赖的自动注入。

**代码演示：**
```java
// 传统方式
UserService userService = new UserService();
UserRepository userRepository = new UserRepository();
userService.setRepository(userRepository);

// IoC + DI方式
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}
```

---

### Q2: Spring中Bean的生命周期是怎样的？

**应答：**
Bean的生命周期主要分为以下几个阶段：

1. **实例化**：容器通过反射创建Bean实例
2. **属性填充**：容器注入Bean的依赖属性
3. **初始化**：
   - 执行BeanPostProcessor的前置处理
   - 执行@PostConstruct标注的初始化方法
   - 执行InitializingBean的afterPropertiesSet()方法
   - 执行init-method指定的方法
   - 执行BeanPostProcessor的后置处理（AOP代理在此生成）
4. **使用**：Bean可以被应用程序使用
5. **销毁**：
   - 执行@PreDestroy标注的销毁方法
   - 执行DisposableBean的destroy()方法
   - 执行destroy-method指定的方法

**面试考点：**
- BeanPostProcessor的作用是什么？（在Bean初始化前后进行自定义处理，AOP代理在此阶段生成）
- @PostConstruct和@PreDestroy的作用？（初始化和销毁回调）

---

### Q3: Spring如何解决循环依赖？三级缓存的作用是什么？

**应答：**
Spring通过三级缓存解决循环依赖问题：

1. **一级缓存（singletonObjects）**：存放完整的单例Bean
2. **二级缓存（earlySingletonObjects）**：存放提前暴露的Bean（未完全初始化）
3. **三级缓存（singletonFactories）**：存放Bean工厂，用于生成提前暴露的Bean

**解决流程：**
1. 创建A实例时，先将A的ObjectFactory放入三级缓存
2. A依赖B，开始创建B
3. B依赖A，从三级缓存获取A的ObjectFactory，创建A的早期引用，放入二级缓存
4. B初始化完成，注入到A中
5. A初始化完成，从二级缓存移除，放入一级缓存

**面试考点：**
- 为什么需要三级缓存？（为了支持AOP代理，避免循环引用时多次创建代理对象）
- 三级缓存的key和value分别是什么？（key：beanName，value：ObjectFactory/Bean实例）

**代码实现：**
```java
// DefaultListableBeanFactory中的三级缓存
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
```

---

### Q4: Bean的作用域有哪些？默认是什么作用域？

**应答：**
Spring支持以下作用域：

1. **singleton**（默认）：单例，整个容器中只有一个实例
2. **prototype**：原型，每次请求都创建新实例
3. **request**：每个HTTP请求创建一个实例（Web环境）
4. **session**：每个HTTP会话创建一个实例（Web环境）
5. **application**：ServletContext生命周期内一个实例（Web环境）

**面试考点：**
- 单例Bean是否线程安全？（不一定，需要开发人员保证线程安全）
- prototype作用域的Bean的生命周期由谁管理？（由调用方管理，容器不管理销毁）

---

## 二、AOP面试题

### Q5: 什么是AOP？AOP的核心概念有哪些？

**应答：**
AOP（面向切面编程）是一种编程范式，通过将横切关注点（如日志、事务、权限）从业务逻辑中分离出来，提高代码的可维护性。

**核心概念：**
1. **切面（Aspect）**：横切关注点的模块化
2. **连接点（JoinPoint）**：程序执行的某个特定位置（如方法调用）
3. **切点（Pointcut）**：匹配连接点的表达式
4. **通知（Advice）**：在切点处执行的代码
5. **织入（Weaving）**：将切面应用到目标对象的过程

**面试考点：**
- AOP解决了什么问题？（代码重复、关注点分离）
- AOP的实现原理是什么？（动态代理）

---

### Q6: JDK动态代理和CGLIB代理有什么区别？

**应答：**

| 特性 | JDK动态代理 | CGLIB代理 |
|------|-------------|------------|
| 实现方式 | 基于接口 | 基于继承（子类） |
| 代理对象 | 实现目标接口 | 继承目标类 |
| 性能 | 较好（JDK 8后优化） | 略差 |
| 使用场景 | 目标类实现接口 | 目标类未实现接口 |
| 限制 | 只能代理接口 | 不能代理final类和方法 |

**Spring选择策略：**
- 目标类实现接口：使用JDK动态代理
- 目标类未实现接口：使用CGLIB代理
- 强制使用CGLIB：@EnableAspectJAutoProxy(proxyTargetClass=true)

**面试考点：**
- 为什么Spring默认使用JDK代理？（性能更好，不破坏面向接口编程）
- CGLIB的原理是什么？（通过字节码生成子类）

---

### Q7: @Around、@Before、@After、@AfterReturning、@AfterThrowing的执行顺序是什么？

**应答：**
正常情况下的执行顺序：
1. @Around（前置）
2. @Before
3. 目标方法执行
4. @AfterReturning
5. @After
6. @Around（后置）

异常情况下的执行顺序：
1. @Around（前置）
2. @Before
3. 目标方法执行（抛出异常）
4. @AfterThrowing
5. @After
6. @Around（异常）

**代码示例：**
```java
@Around("execution(* com.example.*.*(..))")
public Object around(ProceedingJoinPoint pjp) throws Throwable {
    System.out.println("Around before");
    Object result = pjp.proceed();
    System.out.println("Around after");
    return result;
}

@Before("execution(* com.example.*.*(..))")
public void before() {
    System.out.println("Before");
}
```

**面试考点：**
- @Around和@AfterThrowing的区别？（@Around可以捕获异常并处理，@AfterThrowing只能记录）
- @After和@AfterReturning的区别？（@After总是执行，@AfterReturning只在正常返回时执行）

---

## 三、Spring Boot面试题

### Q8: Spring Boot自动配置的原理是什么？

**应答：**
Spring Boot自动配置的核心原理：

1. **@EnableAutoConfiguration**：开启自动配置功能
2. **spring.factories**：配置自动配置类
3. **@Conditional注解**：条件化加载配置

**加载流程：**
1. 启动时，Spring Boot读取`META-INF/spring.factories`文件
2. 加载所有自动配置类
3. 根据@Conditional注解判断是否需要加载
4. 注册符合条件的Bean

**面试考点：**
- 如何自定义Starter？（创建autoconfigure模块，配置spring.factories）
- @ConditionalOnClass的作用？（当类路径存在指定类时生效）

**代码实现：**
```java
@EnableAutoConfiguration
public class WebServerAutoConfiguration {
    
    @ConditionalOnClass(name = "io.netty.channel.Channel")
    @ConditionalOnProperty(name = "web.server.type", havingValue = "netty")
    public NettyWebServer nettyWebServer() {
        return new NettyWebServer();
    }
}
```

---

### Q9: Spring Boot启动流程是怎样的？

**应答：**
SpringApplication.run()的启动流程：

1. **创建SpringApplication对象**：推断Web应用类型、加载初始化器、加载监听器
2. **运行run()方法**：
   - 启动计时器
   - 准备Environment
   - 创建ApplicationContext
   - 初始化Context
   - 刷新Context（加载BeanDefinition、创建Bean）
   - 调用Runner

**关键步骤：**
```java
public ConfigurableApplicationContext run(String... args) {
    // 1. 启动计时器
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    
    // 2. 创建ApplicationContext
    ConfigurableApplicationContext context = createApplicationContext();
    
    // 3. 刷新Context（核心步骤）
    refreshContext(context);
    
    // 4. 调用Runner
    callRunners(context, applicationArguments);
    
    return context;
}
```

**面试考点：**
- ApplicationContext的作用？（Bean容器，管理Bean的生命周期）
- refresh()方法做了什么？（加载配置、注册Bean、初始化Bean）

---

## 四、微服务面试题

### Q10: 服务注册与发现的原理是什么？CAP理论是什么？

**应答：**
服务注册与发现原理：

1. **服务注册**：服务启动时向注册中心注册自己的信息（IP、端口、服务名）
2. **服务发现**：服务消费者从注册中心获取服务提供者列表
3. **心跳机制**：服务定期发送心跳，保持连接
4. **健康检查**：注册中心定期检查服务健康状态，剔除不健康服务

**CAP理论：**
- **C（一致性）**：所有节点同时看到相同的数据
- **A（可用性）**：每个请求都能得到响应
- **P（分区容错性）**：系统在网络分区时仍能继续运行

**面试考点：**
- Eureka是AP还是CP？（AP，优先保证可用性）
- ZooKeeper是AP还是CP？（CP，优先保证一致性）
- 如何保证最终一致性？（定期同步、版本号）

**代码实现：**
```java
public class ServiceRegistry {
    private final Map<String, List<ServiceInstance>> registry = new ConcurrentHashMap<>();
    
    public void register(ServiceInstance instance) {
        registry.computeIfAbsent(instance.getServiceName(), k -> new ArrayList<>())
                .add(instance);
    }
    
    public List<ServiceInstance> discover(String serviceName) {
        return registry.getOrDefault(serviceName, Collections.emptyList());
    }
}
```

---

### Q11: 负载均衡算法有哪些？各自的优缺点是什么？

**应答：**

| 算法 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **轮询** | 简单、公平 | 不考虑服务器性能 | 服务器性能相近 |
| **随机** | 简单、分散请求 | 不保证均匀分配 | 短连接、无状态服务 |
| **加权轮询** | 考虑性能差异 | 配置复杂 | 服务器性能差异大 |
| **最少连接数** | 动态适应 | 需要维护连接数 | 长连接服务 |
| **一致性哈希** | 减少缓存失效 | 实现复杂 | 缓存服务 |

**面试考点：**
- 如何实现加权轮询？（权重累加，选择随机数对应的区间）
- 一致性哈希的应用场景？（分布式缓存、分库分表）

**代码实现：**
```java
public class RoundRobinLoadBalancer implements LoadBalancer {
    private final AtomicInteger index = new AtomicInteger(0);
    
    public ServiceInstance choose(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            throw new IllegalStateException("No instances available");
        }
        int i = index.getAndIncrement() % instances.size();
        return instances.get(i);
    }
}
```

---

### Q12: 熔断器的设计模式是什么？三种状态如何转换？

**应答：**
熔断器采用**状态机模式**，包含三种状态：

1. **闭合（CLOSED）**：
   - 正常状态，请求正常通过
   - 失败率超过阈值时，转换为打开状态

2. **打开（OPEN）**：
   - 熔断状态，直接返回失败
   - 超时后转换为半开状态

3. **半开（HALF_OPEN）**：
   - 尝试恢复状态，允许部分请求通过
   - 成功则转换为闭合，失败则转换为打开

**状态转换图：**
```
CLOSED ──[失败率>阈值]──> OPEN
  ↑                          │
  │                    [超时时间]
  │                          ↓
  └──────[成功]────── HALF_OPEN
              │
              └──[失败]──> OPEN
```

**面试考点：**
- 熔断和降级的区别？（熔断是保护机制，降级是兜底方案）
- 半开状态的作用？（尝试恢复，避免反复熔断）

**代码实现：**
```java
public class CircuitBreaker {
    private State state = State.CLOSED;
    private int failureCount = 0;
    private final int failureThreshold = 50;
    
    public Object execute(Supplier<Object> supplier) {
        if (state == State.OPEN) {
            throw new CircuitBreakerOpenException("Circuit breaker is open");
        }
        
        try {
            Object result = supplier.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
    
    private void onFailure() {
        failureCount++;
        if (failureCount >= failureThreshold) {
            state = State.OPEN;
        }
    }
}
```

---

## 五、JVM调优面试题

### Q13: JVM内存模型是怎样的？各区域的作用是什么？

**应答：**
JVM运行时数据区分为以下部分：

1. **堆（Heap）**：
   - 存放对象实例和数组
   - 分为新生代和老年代
   - GC的主要区域

2. **栈（Stack）**：
   - 存放局部变量、方法参数
   - 线程私有，每个线程一个栈
   - 方法调用和返回使用栈帧

3. **方法区（Method Area）**：
   - 存放类信息、常量、静态变量
   - JDK 8后称为元空间（Metaspace）

4. **程序计数器（Program Counter）**：
   - 存放当前执行的字节码指令地址
   - 线程私有

5. **本地方法栈（Native Method Stack）**：
   - 为Native方法服务

**面试考点：**
- 堆和栈的区别？（堆存放对象，栈存放基本类型和引用）
- 栈溢出和堆溢出的原因？（递归太深、内存泄漏）

**代码验证：**
```java
// 堆溢出
List<byte[]> list = new ArrayList<>();
while (true) {
    list.add(new byte[1024 * 1024]);
}

// 栈溢出
public void stackOverflow() {
    stackOverflow();
}
```

---

### Q14: G1垃圾收集器的原理是什么？如何调优？

**应答：**
G1（Garbage First）收集器特点：

1. **Region划分**：将堆划分为多个大小相等的Region
2. **可预测停顿**：可以设置最大停顿时间目标
3. **增量回收**：不需要一次性回收整个堆
4. **混合GC**：同时回收新生代和老年代

**调优参数：**
- `-XX:+UseG1GC`：启用G1收集器
- `-XX:MaxGCPauseMillis=200`：最大停顿时间200ms
- `-XX:G1HeapRegionSize=16m`：Region大小16MB
- `-XX:InitiatingHeapOccupancyPercent=45`：堆占用45%时启动并发标记

**面试考点：**
- G1和CMS的区别？（G1是增量式，CMS是分代式）
- 如何选择垃圾收集器？（根据应用场景：停顿时间要求、吞吐量要求）

---

### Q15: 如何排查OOM（内存溢出）？常用的工具有哪些？

**应答：**
OOM排查步骤：

1. **启用Heap Dump**：
   ```bash
   -XX:+HeapDumpOnOutOfMemoryError
   -XX:HeapDumpPath=/tmp/heapdump.hprof
   ```

2. **分析Heap Dump**：
   - 使用MAT（Memory Analyzer Tool）打开dump文件
   - 查看Dominator Tree，找到大对象
   - 查看Histogram，统计对象数量

3. **监控内存**：
   ```bash
   jstat -gcutil <pid> 1000 10
   jmap -heap <pid>
   ```

4. **查看线程**：
   ```bash
   jstack <pid> > thread_dump.txt
   ```

**面试考点：**
- MAT中的Dominator Tree是什么？（显示对象占用内存的层级关系）
- 如何判断内存泄漏？（对象数量持续增长、GC后内存不下降）

---

## 六、并发编程面试题

### Q16: 线程池的核心参数有哪些？如何合理配置？

**应答：**
ThreadPoolExecutor的核心参数：

1. **corePoolSize**：核心线程数
2. **maximumPoolSize**：最大线程数
3. **keepAliveTime**：非核心线程空闲时间
4. **workQueue**：任务队列
5. **threadFactory**：线程工厂
6. **handler**：拒绝策略

**配置建议：**
- **CPU密集型**：corePoolSize = CPU核心数 + 1
- **IO密集型**：corePoolSize = CPU核心数 * 2
- **队列大小**：根据业务需求，一般100-1000

**面试考点：**
- 线程池的工作原理？（核心线程满 -> 队列 -> 最大线程 -> 拒绝）
- 拒绝策略有哪些？（AbortPolicy、CallerRunsPolicy、DiscardPolicy）

**代码实现：**
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    8,                              // corePoolSize
    16,                             // maximumPoolSize
    60L, TimeUnit.SECONDS,            // keepAliveTime
    new LinkedBlockingQueue<>(100),     // workQueue
    new BootCloudThreadFactory("pool"), // threadFactory
    new CallerRunsPolicy()             // handler
);
```

---

### Q17: synchronized锁的升级过程是怎样的？

**应答：**
synchronized锁的升级过程：

1. **偏向锁**：
   - 只有一个线程访问时启用
   - 在对象头中记录线程ID
   - 无竞争，性能最好

2. **轻量级锁**：
   - 多个线程交替访问时升级
   - 使用CAS在栈帧中存储锁记录
   - 无竞争时不需要内核介入

3. **重量级锁**：
   - 多线程竞争激烈时升级
   - 使用OS互斥量
   - 需要内核介入，性能最差

**升级条件：**
- 偏向锁 -> 轻量级锁：有其他线程尝试获取锁
- 轻量级锁 -> 重量级锁：CAS失败多次

**面试考点：**
- 为什么需要锁升级？（平衡性能和正确性）
- 如何关闭偏向锁？（-XX:-UseBiasedLocking）

---

### Q18: CAS（Compare-And-Swap）的原理是什么？有什么缺点？

**应答：**
CAS原理：

1. **基本思想**：比较并交换，原子操作
2. **实现方式**：使用CPU的CAS指令（如x86的cmpxchg）
3. **工作流程**：
   - 读取内存值V
   - 计算新值N
   - 比较当前值是否等于期望值A
   - 如果相等，写入N，否则重试

**代码实现：**
```java
public class AtomicCounter {
    private volatile int count;
    
    public void increment() {
        int expected, newValue;
        do {
            expected = count;
            newValue = expected + 1;
        } while (!compareAndSwap(expected, newValue));
    }
    
    private native boolean compareAndSwap(int expected, int newValue);
}
```

**缺点：**
1. **ABA问题**：值从A->B->A，CAS无法检测
2. **循环时间长**：高并发时自旋消耗CPU
3. **只能保证单个变量**：无法保证多个变量的原子性

**面试考点：**
- 如何解决ABA问题？（使用版本号，如AtomicStampedReference）
- 自旋锁和互斥锁的区别？（自旋锁忙等待，互斥锁阻塞）

---

### Q19: 如何检测和避免死锁？

**应答：**
死锁检测方法：

1. **使用jstack**：
   ```bash
   jstack <pid> > thread_dump.txt
   ```

2. **程序检测**：
   ```java
   ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
   long[] deadlockedThreads = threadBean.findDeadlockedThreads();
   ```

3. **死锁预防**：
   - **锁顺序**：所有线程按相同顺序获取锁
   - **锁超时**：使用tryLock()替代lock()
   - **避免嵌套锁**：减少同时持有的锁数量
   - **使用并发工具**：ConcurrentHashMap、AtomicInteger

**死锁四个必要条件**：
1. 互斥条件
2. 请求与保持条件
3. 不剥夺条件
4. 循环等待条件

**面试考点：**
- 如何打破死锁？（打破任一条件即可）
- ReentrantLock如何避免死锁？（tryLock()超时机制）

**代码示例：**
```java
// 死锁预防：按顺序获取锁
private static final Object lock1 = new Object();
private static final Object lock2 = new Object();

public void safeMethod() {
    synchronized (lock1) {
        synchronized (lock2) {
            // 业务逻辑
        }
    }
}
```

---

## 七、综合面试题

### Q20: 在高并发场景下，如何保证系统的稳定性和性能？

**应答：**
高并发系统的优化策略：

1. **架构层面**：
   - 服务拆分：微服务架构，独立扩展
   - 缓存：Redis、本地缓存
   - 异步：消息队列解耦
   - 读写分离：主从数据库

2. **并发层面**：
   - 线程池：合理配置核心参数
   - 锁优化：使用CAS、分段锁
   - 无锁编程：使用原子类

3. **性能层面**：
   - JVM调优：选择合适的GC收集器
   - 数据库优化：索引、分库分表
   - 网络优化：连接池、压缩

4. **稳定性层面**：
   - 熔断降级：Hystrix模式
   - 限流：令牌桶、漏桶算法
   - 监控：Prometheus + Grafana

**面试考点：**
- 如何设计秒杀系统？（库存扣减、限流、异步）
- 如何保证消息不丢失？（确认机制、持久化）

---

**文档版本**: v1.0  
**最后更新**: 2026-03-29  
**维护者**: Boot&Cloud架构团队
