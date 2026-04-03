package com.bootcloud.feign.test;

import com.bootcloud.feign.annotation.FeignClient;
import com.bootcloud.feign.annotation.GetMapping;
import com.bootcloud.feign.annotation.PathVariable;
import com.bootcloud.feign.annotation.PostMapping;
import com.bootcloud.feign.annotation.RequestBody;
import com.bootcloud.feign.core.FeignClientFactory;
import com.bootcloud.registry.core.ServiceDiscovery;
import com.bootcloud.registry.core.ServiceRegistry;
import com.bootcloud.registry.core.impl.DefaultServiceDiscovery;
import com.bootcloud.registry.core.impl.InMemoryServiceRegistry;
import com.bootcloud.registry.model.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feign 与服务注册中心集成测试
 * 
 * 面试考点：
 * 1. 如何测试 Feign 与服务发现的集成？
 *    答：搭建 Mock 注册中心，注册服务实例，验证 Feign 能否正确解析服务名
 * 2. 负载均衡如何验证？
 *    答：注册多个服务实例，多次调用观察是否均匀分布
 * 3. 健康检查如何测试？
 *    答：设置实例 healthy=false，验证不会被选中
 */
public class FeignServiceDiscoveryIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(FeignServiceDiscoveryIntegrationTest.class);
    
    private ServiceRegistry serviceRegistry;
    private ServiceDiscovery serviceDiscovery;
    
    /**
     * 测试用的 Feign 客户端接口（使用服务发现）
     */
    @FeignClient(name = "user-service")
    public interface UserServiceClient {
        @GetMapping(path = "/api/user/{id}")
        User getUserById(@PathVariable("id") Long id);
        
        @PostMapping(path = "/api/user")
        User createUser(@RequestBody User user);
    }
    
    /**
     * 测试用的 Feign 客户端接口（使用固定 URL）
     */
    @FeignClient(name = "", url = "https://jsonplaceholder.typicode.com")
    public interface ExternalServiceClient {
        @GetMapping(path = "/posts/1")
        Post getPost();
    }
    
    @BeforeEach
    public void setUp() {
        logger.info("Setting up test environment...");
        
        // 创建注册中心和服务发现
        serviceRegistry = new InMemoryServiceRegistry(90);
        serviceDiscovery = new DefaultServiceDiscovery(serviceRegistry, 30);
        
        // 注册 3 个 user-service 实例（模拟负载均衡场景）
        ServiceInstance instance1 = new ServiceInstance("user-service", "instance-1", "localhost", 8080);
        ServiceInstance instance2 = new ServiceInstance("user-service", "instance-2", "localhost", 8081);
        ServiceInstance instance3 = new ServiceInstance("user-service", "instance-3", "localhost", 8082);
        
        serviceRegistry.register(instance1);
        serviceRegistry.register(instance2);
        serviceRegistry.register(instance3);
        
        logger.info("Registered 3 instances for user-service");
        
        // 配置 FeignClientFactory 使用服务发现
        FeignClientFactory.setServiceDiscovery(serviceDiscovery);
        
        logger.info("Test environment setup completed");
    }
    
    @AfterEach
    public void tearDown() {
        logger.info("Tearing down test environment...");
        
        if (serviceDiscovery != null) {
            serviceDiscovery.shutdown();
        }
        if (serviceRegistry != null) {
            serviceRegistry.shutdown();
        }
        
        FeignClientFactory.clearCache();
        
        logger.info("Test environment torn down");
    }
    
    @Test
    public void testFeignClientWithServiceDiscovery() {
        logger.info("Testing Feign client creation with service discovery...");
        
        // 创建 Feign 客户端（使用服务发现）
        UserServiceClient client = FeignClientFactory.create(UserServiceClient.class);
        
        assertNotNull(client, "Feign client should be created with service discovery");
        logger.info("Feign client created successfully with service discovery");
    }
    
    @Test
    public void testFeignClientWithFixedUrl() {
        logger.info("Testing Feign client creation with fixed URL...");
        
        // 创建 Feign 客户端（使用固定 URL，不使用服务发现）
        ExternalServiceClient client = FeignClientFactory.create(ExternalServiceClient.class);
        
        assertNotNull(client, "Feign client should be created with fixed URL");
        
        try {
            Post post = client.getPost();
            assertNotNull(post, "Post should be retrieved from external URL");
            logger.info("External service call successful: {}", post);
        } catch (Exception e) {
            logger.warn("External service call failed (may be network issue): {}", e.getMessage());
        }
    }
    
    @Test
    public void testServiceDiscoveryResolution() {
        logger.info("Testing service discovery resolution...");
        
        // 验证服务发现能正确获取实例
        List<ServiceInstance> instances = serviceDiscovery.getInstances("user-service");
        
        assertNotNull(instances, "Service instances should be retrieved");
        assertEquals(3, instances.size(), "Should have 3 instances");
        
        logger.info("Service discovery resolved {} instances", instances.size());
    }
    
    @Test
    public void testLoadBalancing() {
        logger.info("Testing load balancing across instances...");
        
        // 多次选择实例，验证负载均衡
        int[] instanceCounts = new int[3];
        int totalCalls = 100;
        
        for (int i = 0; i < totalCalls; i++) {
            ServiceInstance instance = serviceDiscovery.chooseInstance("user-service");
            assertNotNull(instance, "Should choose an instance");
            
            if (instance.getInstanceId().equals("instance-1")) {
                instanceCounts[0]++;
            } else if (instance.getInstanceId().equals("instance-2")) {
                instanceCounts[1]++;
            } else if (instance.getInstanceId().equals("instance-3")) {
                instanceCounts[2]++;
            }
        }
        
        logger.info("Load balancing results: instance-1={}, instance-2={}, instance-3={}", 
                   instanceCounts[0], instanceCounts[1], instanceCounts[2]);
        
        // 验证每个实例都被选中（随机策略下，100 次调用应该每个实例至少被选中几次）
        assertTrue(instanceCounts[0] > 0, "Instance-1 should be selected at least once");
        assertTrue(instanceCounts[1] > 0, "Instance-2 should be selected at least once");
        assertTrue(instanceCounts[2] > 0, "Instance-3 should be selected at least once");
        
        // 验证分布相对均匀（最大和最小相差不超过 50%）
        int maxCount = Math.max(Math.max(instanceCounts[0], instanceCounts[1]), instanceCounts[2]);
        int minCount = Math.min(Math.min(instanceCounts[0], instanceCounts[1]), instanceCounts[2]);
        assertTrue(maxCount - minCount < totalCalls * 0.5, 
                  "Load should be relatively balanced (max-min < 50% of total)");
        
        logger.info("Load balancing test passed - requests are distributed across instances");
    }
    
    @Test
    public void testHealthCheckFiltering() {
        logger.info("Testing health check filtering...");
        
        // 设置 instance-2 为不健康
        ServiceInstance unhealthyInstance = serviceRegistry.getInstance("user-service", "instance-2");
        assertNotNull(unhealthyInstance);
        unhealthyInstance.setHealthy(false);
        
        // 多次选择实例，验证不会选到不健康的 instance-2
        for (int i = 0; i < 20; i++) {
            ServiceInstance instance = serviceDiscovery.chooseInstance("user-service");
            assertNotNull(instance, "Should choose a healthy instance");
            assertNotEquals("instance-2", instance.getInstanceId(), 
                          "Should not choose unhealthy instance-2");
        }
        
        logger.info("Health check filtering test passed - unhealthy instances are filtered");
    }
    
    @Test
    public void testNoInstanceForUnknownService() {
        logger.info("Testing behavior for unknown service...");
        
        List<ServiceInstance> instances = serviceDiscovery.getInstances("unknown-service");
        
        assertNotNull(instances, "Should return empty list for unknown service");
        assertTrue(instances.isEmpty(), "Should have no instances for unknown service");
        
        logger.info("Unknown service test passed");
    }
    
    @Test
    public void testServiceInstanceDetails() {
        logger.info("Testing service instance details...");
        
        List<ServiceInstance> instances = serviceDiscovery.getInstances("user-service");
        
        assertEquals(3, instances.size(), "Should have 3 instances");
        
        for (ServiceInstance instance : instances) {
            assertEquals("user-service", instance.getServiceId());
            assertNotNull(instance.getInstanceId());
            assertEquals("localhost", instance.getHost());
            assertTrue(instance.getPort() > 0 && instance.getPort() < 65536);
            assertTrue(instance.isHealthy());
            
            logger.info("Instance details: {}:{} (healthy={})", 
                       instance.getHost(), instance.getPort(), instance.isHealthy());
        }
    }
    
    // 测试用的数据模型
    public static class User {
        public Long id;
        public String name;
        public String email;
        
        public User() {}
        
        public User(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
        
        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
        }
    }
    
    public static class Post {
        public Long id;
        public String title;
        public String body;
        public Long userId;
        
        @Override
        public String toString() {
            return "Post{id=" + id + ", title='" + title + "', userId=" + userId + "}";
        }
    }
}
