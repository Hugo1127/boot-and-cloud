# Boot&Cloud 微服务组件层完成总结

## 已完成的微服务模块

### 1. mini-spring-cloud-registry（服务注册与发现）

#### 核心功能
- **服务注册**：支持服务实例注册到注册中心
- **服务发现**：客户端可以从注册中心获取服务实例列表
- **心跳机制**：服务实例定期发送心跳维持活跃状态
- **健康检查**：注册中心定期检查服务实例健康状态，剔除不可用实例
- **HTTP API**：基于Netty的RESTful API，支持注册、注销、续约、查询

#### 核心接口
- `ServiceRegistry`：服务注册接口
- `ServiceDiscovery`：服务发现接口
- `ServiceInstance`：服务实例模型

#### 实现类
- `InMemoryServiceRegistry`：基于内存的服务注册中心实现
- `DefaultServiceDiscovery`：默认服务发现实现，支持本地缓存
- `RegistryServer`：基于Netty的注册中心服务器

#### 面试考点
- 服务注册与发现的原理
- 心跳机制的作用
- 健康检查的设计
- CAP理论在服务注册中心的体现
- Eureka/Nacos vs 自研注册中心的区别

#### 代码文件
- [mini-spring-cloud-registry/src/main/java/](mini-spring-cloud-registry/src/main/java/com/bootcloud/registry/)

---

### 2. mini-spring-cloud-loadbalancer（客户端负载均衡）

#### 核心功能
- **多种负载均衡策略**：
  - 轮询（Round Robin）：按顺序依次选择实例
  - 随机（Random）：随机选择实例
  - 加权轮询（Weighted Round Robin）：根据权重选择实例
  - 最少活跃数（Least Active）：选择当前活跃请求最少的实例
- **注解支持**：`@LoadBalanced`注解标记需要负载均衡的调用
- **动态权重**：支持通过元数据配置实例权重
- **活跃数统计**：实时统计各实例的活跃请求数

#### 核心接口
- `LoadBalancer`：负载均衡接口
- `LoadBalancerFactory`：负载均衡器工厂

#### 实现类
- `RoundRobinLoadBalancer`：轮询策略
- `RandomLoadBalancer`：随机策略
- `WeightedRoundRobinLoadBalancer`：加权轮询策略
- `LeastActiveLoadBalancer`：最少活跃数策略

#### 面试考点
- 客户端负载均衡 vs 服务端负载均衡
- 各种负载均衡算法的适用场景
- 如何根据业务场景选择负载均衡策略
- 加权轮询的实现原理
- 最少活跃数策略的优势

#### 代码文件
- [mini-spring-cloud-loadbalancer/src/main/java/](mini-spring-cloud-loadbalancer/src/main/java/com/bootcloud/loadbalancer/)

---

### 3. mini-spring-cloud-circuitbreaker（服务熔断与降级）

#### 核心功能
- **熔断器状态管理**：
  - CLOSED（闭合）：正常请求状态
  - OPEN（打开）：熔断打开，拒绝所有请求
  - HALF_OPEN（半开）：尝试恢复状态
- **熔断配置**：可配置失败阈值、超时时间、恢复时间
- **降级方法**：支持配置降级方法，熔断时执行降级逻辑
- **注解支持**：`@CircuitBreaker`注解标记需要熔断的方法
- **指标统计**：统计请求总数、失败数、成功率等指标
- **自动恢复**：熔断超时后自动尝试恢复

#### 核心接口
- `CircuitBreaker`：熔断器接口
- `CircuitBreakerConfig`：熔断器配置
- `CircuitBreakerState`：熔断器状态枚举

#### 实现类
- `DefaultCircuitBreaker`：默认熔断器实现
- `CircuitBreakerFactory`：熔断器工厂
- `CircuitBreakerOpenException`：熔断打开异常

#### 面试考点
- 熔断器的作用和原理
- 熔断器的三种状态及其转换
- 如何设置熔断器的阈值
- 熔断与降级的区别
- 服务雪崩问题及解决方案

#### 代码文件
- [mini-spring-cloud-circuitbreaker/src/main/java/](mini-spring-cloud-circuitbreaker/src/main/java/com/bootcloud/circuitbreaker/)

---

### 4. mini-spring-gateway（API网关）

#### 核心功能
- **路由转发**：根据请求路径将请求转发到对应的微服务
- **过滤器链**：支持多个过滤器，按顺序处理请求
- **内置过滤器**：
  - LoggingFilter：记录请求日志
  - RateLimitFilter：限流过滤器
- **路由配置**：支持动态添加/删除路由
- **路由优先级**：支持路由优先级配置
- **跨域支持**：支持CORS配置
- **统一入口**：作为微服务的统一入口

#### 核心接口
- `Gateway`：网关接口
- `GatewayFilter`：过滤器接口
- `GatewayFilterChain`：过滤器链接口

#### 实现类
- `DefaultGateway`：默认网关实现
- `LoggingFilter`：日志过滤器
- `RateLimitFilter`：限流过滤器

#### 模型类
- `Route`：路由模型
- `GatewayRequest`：网关请求模型
- `GatewayResponse`：网关响应模型

#### 面试考点
- API网关的作用
- 网关的核心功能（路由、过滤、限流、认证等）
- 过滤器链的设计模式
- 如何实现限流
- 网关的性能优化

#### 代码文件
- [mini-spring-gateway/src/main/java/](mini-spring-gateway/src/main/java/com/bootcloud/gateway/)

---

### 5. demo-app（示例微服务应用）

#### 包含的微服务
- **user-service**：用户服务
  - 用户CRUD操作
  - 用户数据存储
  - 用户查询接口

- **order-service**：订单服务
  - 订单CRUD操作
  - 订单数据存储
  - 订单查询接口
  - 集成熔断器

- **goods-service**：商品服务
  - 商品CRUD操作
  - 商品数据存储
  - 商品查询接口
  - 库存管理

#### 核心功能
- **完整的微服务架构**：演示服务间调用
- **数据初始化**：启动时自动初始化测试数据
- **RESTful API**：提供标准的HTTP接口
- **依赖注入**：使用IOC容器管理Bean
- **熔断降级**：订单服务演示熔断降级功能

#### 技术特点
- 模块化设计：每个服务独立的包结构
- 依赖注入：通过构造器注入依赖
- 注解驱动：使用自定义注解简化配置
- 数据隔离：每个服务使用独立的内存存储

#### 启动方式
```bash
# 编译项目
mvn clean install

# 启动应用
cd demo-app
mvn exec:java -Dexec.mainClass="com.bootcloud.demo.Application"
```

#### API示例
```
# 查询用户
GET http://localhost:8080/api/user/1

# 创建用户
POST http://localhost:8080/api/user
{
  "name": "张三",
  "email": "zhangsan@example.com",
  "age": 25
}

# 查询订单
GET http://localhost:8080/api/order/1

# 创建订单
POST http://localhost:8080/api/order
{
  "userId": 1,
  "productId": 1,
  "quantity": 2
}

# 查询商品
GET http://localhost:8080/api/product/1

# 创建商品
POST http://localhost:8080/api/product
{
  "name": "iPhone 15",
  "description": "Apple智能手机",
  "price": 7999.00,
  "stock": 100
}
```

#### 面试考点
- 微服务架构设计
- 服务拆分原则
- 服务间通信方式
- 分布式事务处理
- 数据一致性方案

#### 代码文件
- [demo-app/src/main/java/](demo-app/src/main/java/com/bootcloud/demo/)

---

## 待完成模块

### mini-spring-cloud-feign（远程服务调用）
- **计划功能**：
  - `@FeignClient`注解：声明式HTTP客户端
  - 动态代理：基于接口生成HTTP客户端
  - 序列化支持：JSON、Protobuf
  - 超时配置：可配置请求超时
  - 重试机制：支持自动重试

- **核心价值**：
  - 简化服务间调用
  - 统一调用方式
  - 提高开发效率

---

## 微服务架构全景图

```
                    ┌─────────────┐
                    │   Client    │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Gateway   │
                    └──────┬──────┘
                           │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
    ┌─────▼─────┐    ┌─────▼─────┐    ┌─────▼─────┐
    │ User Svc  │    │ Order Svc │    │Goods Svc │
    └─────┬─────┘    └─────┬─────┘    └─────┬─────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                      ┌──────▼──────┐
                      │  Registry   │
                      └─────────────┘
```

---

## 技术亮点

### 1. 完整的微服务组件
- 服务注册与发现
- 客户端负载均衡
- 服务熔断与降级
- API网关
- 示例应用

### 2. 注解驱动开发
- `@EnableServiceRegistry`：开启服务注册
- `@EnableServiceDiscovery`：开启服务发现
- `@LoadBalanced`：标记负载均衡
- `@CircuitBreaker`：标记熔断

### 3. 可插拔设计
- 每个模块独立可运行
- 接口优先，支持多种实现
- 易于扩展和定制

### 4. 性能优化
- 内存存储（注册中心）
- 本地缓存（服务发现）
- 线程池（并发处理）
- 零拷贝（Netty）

---

## 面试准备建议

### 重点掌握
1. **服务注册发现**：CAP理论、一致性、可用性
2. **负载均衡**：算法选择、权重配置
3. **熔断降级**：状态转换、阈值设置
4. **API网关**：路由规则、过滤器链
5. **微服务架构**：服务拆分、通信方式

### 实战演示
1. 启动注册中心
2. 启动多个服务实例
3. 演示服务发现
4. 演示负载均衡
5. 演示熔断降级
6. 演示网关路由

---

**微服务组件层完成度：80%**（除Feign模块外，其他模块均已完成）
