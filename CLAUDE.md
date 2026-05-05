# CLAUDE.md — Boot&Cloud 项目开发规范

> 手写极简 Java 微服务框架，深度复刻 Spring Boot + Spring Cloud 核心能力。
> 本文档为项目永久开发约束，**所有后续开发（含 AI 辅助）必须严格遵守**。

---

## 1. 项目定位

- **名称**：Boot&Cloud
- **目标**：从零手写一个极简但完整的 Java 微服务框架，用于 **面试备战** 与 **原理学习**
- **非生产项目**：代码以"原理展示 + 面试演示"为导向

---

## 2. 技术栈（强制）

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | **25** | 不得低于 17 |
| 构建工具 | Maven 3.x | 多模块聚合 |
| Netty | 4.1.104 | 仅用于网络通信层 |
| Jackson | 2.15.2 | 唯一 JSON 序列化库 |
| JUnit | 5.10.0 | 唯一测试框架 |
| SLF4J + Logback | 2.0.9 / 1.4.11 | 唯一日志方案 |
| CGLIB | 3.3.0 | AOP 动态代理（无接口类） |
| ASM | 9.6 | 字节码操作 |

### 2.1 禁止项

- **严禁**引入任何 Spring 全家桶依赖
- **严禁**引入第三方 IOC/AOP 框架
- **严禁**引入成熟服务治理组件（Eureka、Nacos、Hystrix、Sentinel 等）
- **严禁**在代码中写"面试相关"注释（考点放 INTERVIEW_QUESTIONS.md）
- 核心逻辑必须 **从零自研**

---

## 3. 模块架构

```
boot-cloud-parent (聚合父)
├── mini-spring-core          # IOC + AOP + 事件
├── mini-spring-boot          # 自动配置 + 嵌入式 Netty
├── mini-spring-cloud-registry # 服务注册与发现
├── mini-spring-cloud-feign   # 声明式 HTTP 客户端
├── mini-spring-cloud-loadbalancer # 客户端负载均衡
├── mini-spring-cloud-circuitbreaker # 熔断器
├── mini-spring-gateway       # API 网关
├── jvm-optimizer             # JVM 监控与调优
├── concurrent-optimizer      # 线程池 + 锁 + AQS/CAS
└── demo-app                  # 示例应用（user/order/goods）
```

### 3.1 模块依赖（严禁循环）

```
demo-app → 所有其他模块
mini-spring-boot → mini-spring-core
mini-spring-gateway → mini-spring-boot, mini-spring-cloud-registry
mini-spring-cloud-feign → mini-spring-cloud-registry, mini-spring-cloud-loadbalancer
mini-spring-cloud-loadbalancer → mini-spring-cloud-registry
mini-spring-cloud-circuitbreaker → 无依赖
jvm-optimizer → 无依赖
concurrent-optimizer → 无依赖
```

---

## 4. 编码规范

### 4.1 命名

- 包：`com.bootcloud.{module}`，子包 `annotation`, `core`, `core.impl`, `factory` 等
- 接口：简单名词（`BeanFactory`, `LoadBalancer`）
- 实现类：`Default` / `Impl` 前缀（`DefaultListableBeanFactory`）
- 工具类：`XxxUtils`
- 异常：`XxxException`（继承 `RuntimeException`）

### 4.2 注释

- **核心代码必须有注释**，说明底层原理和实现思路
- 每个类顶部说明职责
- 关键算法注释时间/空间复杂度
- **不写"面试相关"注释**

### 4.3 并发安全

- 共享可变状态必须线程安全（`ConcurrentHashMap`, `AtomicInteger` 等）
- 复合操作（check-then-act）必须加锁或原子操作
- 优先 per-bean-lock 而非全局锁
- AQS 操作必须保证 `park()` / `unpark()` 正确配对

---

## 5. 构建与测试

```bash
# 全量编译
mvn clean install

# 跳过测试
mvn clean install -DskipTests

# 单个模块
mvn clean install -pl mini-spring-core

# 运行测试
mvn test -pl concurrent-optimizer
```

### 5.1 测试规范

- 命名：`*Test.java` 或 `*Tests.java`
- 目标覆盖率：**≥ 80%**
- 每个核心类至少一个测试
- JDK 编译目标：JDK 25

---

## 6. 设计约定

### 6.1 Bean 生命周期

实例化 → 暴露早期引用（循环依赖） → 属性填充（@Autowired） → 初始化（@PostConstruct） → 注册销毁回调（@PreDestroy）

### 6.2 AOP 通知执行顺序（期望）

Around(前置) → Before → 目标方法 → Around(后置) → After → AfterReturning

### 6.3 熔断状态机

CLOSED（正常）→ 失败达阈值 → OPEN（拒绝）→ resetTimeout 到 → HALF_OPEN（试探）→ 成功 → CLOSED / 失败 → OPEN

---

## 7. 设计约束（不可改动）

| 约束 | 说明 |
|---|---|
| 三级缓存 | `singletonObjects` → `earlySingletonObjects` → `singletonFactories` |
| per-bean-lock | `ReentrantLock` 按 bean name 独立 |
| BeanDefinition 结构 | beanName, beanClass, instance, constructor, autowiredFields, autowiredMethods, postConstructMethods, preDestroyMethods |
| 注解元数据 | `@Service`/`@Repository`/`@Controller` 均 meta-annotated with `@Component` |
| Netty 线程模型 | Boss(1) + Worker |
| 模块依赖方向 | registry → loadbalancer → feign 不可打破 |

### 7.1 已知待优化项（来自 TODO.md）

| 问题 | 优先级 | 状态 |
|---|---|---|
| Web 服务器未自动启动 | P0 | 待修复 |
| @PathVariable 未实现解析 | P0 | 待修复 |
| 缺少 @Value 注解和配置文件加载 | P0 | 待实现 |
| AOP 未集成到 Bean 生命周期 | P0 | 待重构 |
| AOP 通知执行顺序不正确 | P0 | 待重构 |
| AutoConfigurationLoader 条件判断运算符优先级 | P0 | 待修复 |
| 缺少 mini-spring-boot 单元测试 | P0 | 待补充 |
| Environment 为内部类 | P1 | 待抽取 |
| 缺少 BeanPostProcessor 机制 | P1 | 待实现 |
| 缺少 @AfterReturning / @AfterThrowing | P1 | 待实现 |

---

## 8. 新增模块约束

- 必须在 `boot-cloud-parent/pom.xml` 的 `<modules>` 中注册
- 必须有独立 `pom.xml`，继承父 POM
- 包名遵循 `com.bootcloud.{module}`
- 接口与实现分离（`core/` + `core.impl/`）
- 必须在 `src/test/` 下提供单元测试
- 必须在 TODO.md 中登记状态

---

> **文档维护**：每次完成新功能或修复 Bug 后，同步更新本文档和 TODO.md。
> **最后更新**：2026-05-05
