package com.bootcloud.loadbalancer.test;

import com.bootcloud.loadbalancer.core.LoadBalancer;
import com.bootcloud.loadbalancer.factory.LoadBalancerFactory;
import com.bootcloud.loadbalancer.core.impl.LeastActiveLoadBalancer;
import com.bootcloud.loadbalancer.core.impl.RandomLoadBalancer;
import com.bootcloud.loadbalancer.core.impl.RoundRobinLoadBalancer;
import com.bootcloud.loadbalancer.core.impl.WeightedRoundRobinLoadBalancer;
import com.bootcloud.registry.model.ServiceInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LoadBalancerTest {
    private List<ServiceInstance> instances;

    @BeforeEach
    public void setUp() {
        instances = new ArrayList<>();
        
        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put("weight", "2");
        instances.add(new ServiceInstance("user-service", "instance-1", "localhost", 8080));
        instances.get(0).setMetadata(metadata1);
        
        Map<String, String> metadata2 = new HashMap<>();
        metadata2.put("weight", "1");
        instances.add(new ServiceInstance("user-service", "instance-2", "localhost", 8081));
        instances.get(1).setMetadata(metadata2);
        
        instances.add(new ServiceInstance("user-service", "instance-3", "localhost", 8082));
    }

    @Test
    public void testRoundRobinLoadBalancer() {
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        
        assertEquals("RoundRobin", loadBalancer.getStrategyName());
        
        // 测试轮询应该按顺序返回实例
        ServiceInstance instance1 = loadBalancer.choose(instances);
        ServiceInstance instance2 = loadBalancer.choose(instances);
        ServiceInstance instance3 = loadBalancer.choose(instances);
        ServiceInstance instance4 = loadBalancer.choose(instances);
        
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);
        assertNotNull(instance4);
        
        // 第 4 个应该和第 1 个相同（轮询一圈）
        assertEquals(instance1.getInstanceId(), instance4.getInstanceId());
        
        // 所有实例都应该被选中
        assertTrue(instances.contains(instance1));
        assertTrue(instances.contains(instance2));
        assertTrue(instances.contains(instance3));
    }

    @Test
    public void testRandomLoadBalancer() {
        LoadBalancer loadBalancer = new RandomLoadBalancer();
        
        assertEquals("Random", loadBalancer.getStrategyName());
        
        for (int i = 0; i < 100; i++) {
            ServiceInstance instance = loadBalancer.choose(instances);
            assertNotNull(instance);
            assertTrue(instances.contains(instance));
        }
    }

    @Test
    public void testWeightedRoundRobinLoadBalancer() {
        LoadBalancer loadBalancer = new WeightedRoundRobinLoadBalancer();
        
        assertEquals("WeightedRoundRobin", loadBalancer.getStrategyName());
        
        Map<String, Integer> countMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            ServiceInstance instance = loadBalancer.choose(instances);
            countMap.merge(instance.getInstanceId(), 1, Integer::sum);
        }
        
        assertTrue(countMap.get("instance-1") > 30);
        assertTrue(countMap.get("instance-2") > 15);
        assertTrue(countMap.get("instance-3") > 15);
    }

    @Test
    public void testLeastActiveLoadBalancer() {
        LeastActiveLoadBalancer loadBalancer = new LeastActiveLoadBalancer();
        
        assertEquals("LeastActive", loadBalancer.getStrategyName());
        
        ServiceInstance instance1 = loadBalancer.choose(instances);
        ServiceInstance instance2 = loadBalancer.choose(instances);
        
        assertNotNull(instance1);
        assertNotNull(instance2);
        
        loadBalancer.completeRequest(instance1.getInstanceId());
        
        ServiceInstance instance3 = loadBalancer.choose(instances);
        assertEquals(instance1.getInstanceId(), instance3.getInstanceId());
    }

    @Test
    public void testLoadBalancerFactory() {
        LoadBalancer roundRobin = LoadBalancerFactory.createLoadBalancer("roundrobin");
        assertTrue(roundRobin instanceof RoundRobinLoadBalancer);
        
        LoadBalancer random = LoadBalancerFactory.createLoadBalancer("random");
        assertTrue(random instanceof RandomLoadBalancer);
        
        LoadBalancer weighted = LoadBalancerFactory.createLoadBalancer("weighted");
        assertTrue(weighted instanceof WeightedRoundRobinLoadBalancer);
        
        LoadBalancer leastActive = LoadBalancerFactory.createLoadBalancer("leastactive");
        assertTrue(leastActive instanceof LeastActiveLoadBalancer);
        
        LoadBalancer defaultBalancer = LoadBalancerFactory.createLoadBalancer("unknown");
        assertTrue(defaultBalancer instanceof RoundRobinLoadBalancer);
    }

    @Test
    public void testEmptyInstances() {
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        assertNull(loadBalancer.choose(new ArrayList<>()));
    }

    @Test
    public void testNullInstances() {
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        assertNull(loadBalancer.choose(null));
    }
}
