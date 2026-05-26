# Feign 与服务注册中心集成使用示例

## 功能概述

Feign 模块现已集成服务注册中心（ServiceDiscovery）和负载均衡（LoadBalancer）功能，支持：

1. **服务发现调用** - 通过服务名自动解析为实际地址
2. **负载均衡** - 自动在多个服务实例间分配请求
3. **健康检查** - 自动过滤不健康的服务实例
4. **固定 URL 调用** - 兼容原有的直接 URL 调用方式

## 使用方式

### 1. 配置服务发现

```java
// 创建注册中心和服务发现
ServiceRegistry serviceRegistry = new InMemoryServiceRegistry(90);
ServiceDiscovery serviceDiscovery = new DefaultServiceDiscovery(serviceRegistry, 30);

// 注册服务实例
ServiceInstance instance1 = new ServiceInstance("user-service", "instance-1", "localhost", 8080);
ServiceInstance instance2 = new ServiceInstance("user-service", "instance-2", "localhost", 8081);
serviceRegistry.register(instance1);
serviceRegistry.register(instance2);

// 配置 FeignClientFactory 使用服务发现
FeignClientFactory.setServiceDiscovery(serviceDiscovery);
```

### 2. 定义 Feign 客户端（使用服务发现）

```java
@FeignClient(name = "user-service")  // 使用服务名，自动从注册中心获取地址
public interface UserServiceClient {
    @GetMapping(path = "/api/user/{id}")
    User getUserById(@PathVariable("id") Long id);
    
    @PostMapping(path = "/api/user")
    User createUser(@RequestBody User user);
}
```

### 3. 定义 Feign 客户端（使用固定 URL）

```java
@FeignClient(name = "", url = "http://localhost:8080")  // 直接指定 URL
public interface ExternalServiceClient {
    @GetMapping(path = "/api/data")
    Data getData();
}
```

### 4. 使用示例

```java
// 创建客户端（自动使用服务发现）
UserServiceClient client = FeignClientFactory.create(UserServiceClient.class);

// 调用远程服务（自动负载均衡）
User user = client.getUserById(1L);

// 多次调用会均匀分布到不同实例
for (int i = 0; i < 10; i++) {
    User user = client.getUserById((long)i);
}
```

## 核心实现原理

### 服务名解析流程

```
@FeignClient(name = "user-service")
    ↓
FeignInvocationHandler.buildUrl(path)
    ↓
ServiceDiscovery.getInstances("user-service")
    ↓
LoadBalancer.choose(instances)  // 负载均衡选择实例
    ↓
http://host:port + path  // 构建完整 URL
```

### 负载均衡策略

默认使用**轮询策略（RoundRobin）**，支持以下策略：

- `roundRobin` - 轮询（默认）
- `random` - 随机
- `weighted` - 权重
- `leastActive` - 最少活跃数

### 健康检查

自动过滤 `healthy=false` 的实例，确保只调用健康的服务。

## 面试考点

### 1. Feign 如何与服务注册中心集成？

**答**：通过 `ServiceDiscovery` 获取服务实例列表，结合 `LoadBalancer` 选择实例：

```java
// FeignInvocationHandler.chooseInstance()
List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
List<ServiceInstance> healthyInstances = instances.stream()
    .filter(ServiceInstance::isHealthy)
    .toList();
ServiceInstance chosen = loadBalancer.choose(healthyInstances);
```

### 2. 服务名如何转换为实际 URL？

**答**：从 ServiceDiscovery 缓存中获取服务实例，拼接 `http://host:port + path`：

```java
String baseUrl = "http://" + instance.getHost() + ":" + instance.getPort();
String url = baseUrl + path;
```

### 3. 负载均衡在何时生效？

**答**：在 `chooseInstance()` 时生效，每次调用都会通过 LoadBalancer 从多个实例中选择一个。

### 4. 如何保证高可用？

**答**：
- 多实例注册 - 服务注册多个实例
- 健康检查 - 自动过滤不健康实例
- 负载均衡 - 均匀分配请求，避免单点过载

## 测试示例

运行集成测试验证功能：

```bash
cd mini-spring-cloud-feign
mvn test -Dtest=FeignServiceDiscoveryIntegrationTest
```

测试覆盖：
- ✅ Feign 客户端创建（服务发现模式）
- ✅ Feign 客户端创建（固定 URL 模式）
- ✅ 服务发现解析
- ✅ 负载均衡分布
- ✅ 健康检查过滤
- ✅ 未知服务处理

## 注意事项

1. **必须配置 ServiceDiscovery** - 使用服务发现模式时，必须通过 `FeignClientFactory.setServiceDiscovery()` 配置
2. **服务名必须匹配** - `@FeignClient(name = "xxx")` 的服务名必须与注册中心的服务名一致
3. **健康实例必须存在** - 如果没有健康实例，会抛出 `RuntimeException`
4. **兼容旧版本** - 保留了无参构造函数，向后兼容

## 架构图

```
┌─────────────┐
│ Feign Client│
│  Interface  │
└──────┬──────┘
       │
       ↓
┌─────────────────────────────┐
│ FeignInvocationHandler      │
│  - buildUrl()               │
│  - chooseInstance()         │
└──────┬──────────────────────┘
       │
       ├──────────────┐
       ↓              ↓
┌─────────────┐  ┌────────────┐
│ Service     │  │ LoadBalancer│
│ Discovery   │  │ (RoundRobin)│
└──────┬──────┘  └────────────┘
       │
       ↓
┌─────────────────┐
│ ServiceInstance │
│ - host:port     │
│ - healthy       │
└─────────────────┘
```
