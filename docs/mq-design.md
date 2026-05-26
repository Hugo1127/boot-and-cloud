# mini-spring-cloud-mq — 设计文档

> 从零手写一个轻量级消息队列，类似 RabbitMQ/Kafka 的核心概念，
> 用于演示消息中间件的核心原理，服务于面试备战。

---

## 1. 定位与目标

| 维度 | 说明 |
|---|---|
| **定位** | 轻量级嵌入式消息队列，展示 MQ 核心原理 |
| **非生产** | 不追求高性能持久化，以"原理演示 + 面试演示"为目标 |
| **参考** | RabbitMQ（Exchange/Queue/RoutingKey）+ Kafka（ConsumerGroup/Offset） |
| **核心能力** | 消息路由、生产/消费、消息确认、防止重复消费、死信队列 |

---

## 2. 模块架构

```
mini-spring-cloud-mq
├── api/                          # 公共接口与模型
│   ├── Message.java              # 消息体（id, body, headers, timestamp）
│   ├── Exchange.java             # 交换机接口
│   ├── Queue.java                # 队列接口
│   ├── MessageBroker.java        # Broker 总入口
│   ├── Producer.java             # 生产者接口
│   ├── Consumer.java             # 消费者接口
│   └── MessageListener.java      # 消息监听回调
├── core/                         # 核心实现
│   ├── BrokerImpl.java           # Broker 主实现
│   ├── exchange/
│   │   ├── DirectExchange.java   # 精确匹配路由
│   │   ├── TopicExchange.java    # 通配符路由（* / #）
│   │   └── FanoutExchange.java   # 广播路由
│   ├── queue/
│   │   ├── MessageQueue.java     # 基于 MyBlockingQueue 的消息队列
│   │   └── DeadLetterQueue.java  # 死信队列
│   ├── routing/
│   │   └── RoutingTable.java     # Exchange ↔ Queue 绑定表
│   └── idempotent/
│       ├── IdempotentChecker.java # 幂等性检查接口
│       └── BloomFilterChecker.java # 布隆过滤器实现
├── consumer/                     # 消费者模型
│   ├── ConsumerGroup.java        # 消费者组
│   ├── OffsetManager.java        # 偏移量管理
│   └── PullConsumer.java         # 拉取模式消费者
│   └── PushConsumer.java         # 推送模式消费者（回调驱动）
├── net/                          # 网络通信层（可选）
│   ├── MqServer.java             # Netty 服务端
│   └── MqClient.java             # Netty 客户端
├── annotation/                   # 注解
│   ├── @MqListener.java          # 标注在方法上，自动注册消费者
│   └── @EnableMq.java            # 启用 MQ 自动配置
└── autoconfigure/                # Boot 自动配置
    └── MqAutoConfiguration.java  # 自动装配 Broker、Exchange、Queue
```

---

## 3. 核心概念

### 3.1 消息模型

```
Message
├── messageId     # UUID / Snowflake，全局唯一
├── body          # byte[]，消息体
├── headers       # Map<String, String>，扩展头
├── routingKey    # 路由键
├── timestamp     # 发送时间戳
├── retryCount    # 重试次数
└── maxRetry      # 最大重试次数
```

### 3.2 Exchange 交换机（参考 RabbitMQ）

| 类型 | 路由规则 | 场景 |
|---|---|---|
| **Direct** | routingKey 精确匹配 | 单播、定向投递 |
| **Topic** | routingKey 通配符匹配（`*` 匹配一个词，`#` 匹配零到多个词） | 多播、按主题订阅 |
| **Fanout** | 忽略 routingKey，广播到所有绑定队列 | 公告、事件广播 |

### 3.3 队列

- 底层使用手写的 `MyBlockingQueue<Message>` 作为存储
- 支持配置：容量上限、是否持久化、是否独占
- 每个 Queue 关联一个 DeadLetterQueue（死信队列）

### 3.4 消费者模型

| 模式 | 说明 |
|---|---|
| **Pull（拉取）** | 消费者主动 `poll()` 拉取消息，自行控制消费节奏 |
| **Push（推送）** | Broker 回调 `MessageListener.onMessage()`，自动投递 |

### 3.5 消费者组（参考 Kafka）

- 多个消费者加入同一个 `ConsumerGroup`
- 队列中的每条消息只被组内**一个消费者**消费（类似 Kafka partition 分配）
- 不同消费者组之间互不影响（每条消息会被每个组消费一次）

---

## 4. 关键问题与解决方案

### 4.1 防止重复消费（幂等性）

**问题**：网络重传、ACK 丢失等场景下，同一条消息可能被投递多次。

**方案**：

```
布隆过滤器（Bloom Filter）
├── 生产者发送消息时，将 messageId 写入布隆过滤器
├── Broker 投递前检查：若 messageId 已存在，则跳过投递
├── 优点：空间效率极高，O(1) 查询
└── 缺点：存在误判（可能把新 ID 误判为已存在），但不漏判
```

备选方案（接口层面支持切换）：

- **Redis Set**：`messageId` 存入 Set，通过 `SADD` 原子操作判断（需要外部依赖）
- **数据库唯一索引**：通过唯一约束防重（需要外部依赖）

本项目默认实现布隆过滤器方案，零外部依赖。

### 4.2 消息确认（ACK）

```
消费者收到消息 → 业务处理 → 返回 ACK
├── 成功 → 从队列中移除
├── 失败 → retryCount++
│   ├── retryCount < maxRetry → 重新入队
│   └── retryCount >= maxRetry → 转入死信队列
└── 超时未 ACK → 自动重新入队（NACK）
```

### 4.3 消息顺序性

- **保证**：同一 Queue 内，消息严格按入队顺序投递
- **不保证**：多 Queue 并发消费的全局顺序
- 消费者侧串行处理（单线程消费）可保证单 Queue 内有序

### 4.4 消息堆积与背压

- `MyBlockingQueue` 有容量上限，满时 `put()` 阻塞生产者
- 可配置拒绝策略：阻塞等待 / 丢弃 / 转入溢出队列

---

## 5. 与 Boot 集成

### 5.1 注解驱动

```java
@EnableMq  // 启动类开启
public class Application { }

@MqListener(queue = "order.queue", group = "order-group")
public class OrderConsumer {
    public void onMessage(Message msg) {
        // 处理消息
    }
}
```

### 5.2 自动配置

```java
// MqAutoConfiguration.java
@Bean
public MessageBroker messageBroker() { return new BrokerImpl(); }

@Bean
public Exchange orderExchange() { return new TopicExchange("order.exchange"); }

@Bean
public MessageQueue orderQueue() { return new MessageQueue("order.queue", 1000); }
```

---

## 6. 依赖关系

```
mini-spring-cloud-mq → mini-spring-boot, mini-spring-core, concurrent-optimizer(MyBlockingQueue)
```

不依赖外部中间件，纯内存 + 手写组件实现。

---

## 7. 测试计划

| 测试场景 | 验证点 |
|---|---|
| Direct Exchange 路由 | routingKey 精确匹配 |
| Topic Exchange 路由 | `*` 和 `#` 通配符匹配 |
| Fanout Exchange 路由 | 广播到所有绑定队列 |
| 生产/消费 | 消息正确投递 |
| ACK 机制 | 确认后移除，失败重试 |
| 死信队列 | 超过最大重试次数转入 DLQ |
| 幂等性 | 布隆过滤器防重复消费 |
| 消费者组 | 组内单消费者，组间独立 |
| Push vs Pull | 两种模式都能正常工作 |
| 消息堆积 | 队列满时阻塞生产者 |

---

## 8. 开发阶段

### Phase 1：核心模型（P0）
- Message、Exchange、Queue、Broker 基础接口与实现
- 三种 Exchange 路由逻辑
- 基于 `MyBlockingQueue` 的队列实现
- 生产/消费基本流程

### Phase 2：可靠性保障（P0）
- ACK 机制
- 死信队列
- 布隆过滤器幂等性检查
- 重试机制

### Phase 3：消费者模型（P1）
- PullConsumer 实现
- PushConsumer 实现
- ConsumerGroup 实现
- OffsetManager 实现

### Phase 4：Boot 集成（P1）
- `@MqListener` / `@EnableMq` 注解
- `MqAutoConfiguration` 自动装配
- 与 `mini-spring-boot` 集成

### Phase 5：网络通信层（P2，可选）
- Netty 服务端（MqServer）
- Netty 客户端（MqClient）
- 协议设计（简单二进制协议）

### Phase 6：测试与文档（P1）
- 单元测试覆盖 ≥ 80%
- 集成测试
- docs/interview-questions.md 补充 MQ 考点
