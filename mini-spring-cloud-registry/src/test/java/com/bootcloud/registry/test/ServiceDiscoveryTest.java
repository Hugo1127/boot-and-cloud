package com.bootcloud.registry.test;

import com.bootcloud.registry.core.ServiceDiscovery;
import com.bootcloud.registry.core.ServiceRegistry;
import com.bootcloud.registry.core.impl.DefaultServiceDiscovery;
import com.bootcloud.registry.core.impl.InMemoryServiceRegistry;
import com.bootcloud.registry.model.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceDiscoveryTest {
    private ServiceRegistry serviceRegistry;
    private ServiceDiscovery serviceDiscovery;

    @BeforeEach
    public void setUp() {
        serviceRegistry = new InMemoryServiceRegistry(90);
        serviceDiscovery = new DefaultServiceDiscovery(serviceRegistry, 30);
        
        ServiceInstance instance1 = new ServiceInstance("user-service", "instance-1", "localhost", 8080);
        ServiceInstance instance2 = new ServiceInstance("user-service", "instance-2", "localhost", 8081);
        ServiceInstance instance3 = new ServiceInstance("order-service", "instance-3", "localhost", 8082);
        
        serviceRegistry.register(instance1);
        serviceRegistry.register(instance2);
        serviceRegistry.register(instance3);
    }

    @AfterEach
    public void tearDown() {
        serviceDiscovery.shutdown();
        serviceRegistry.shutdown();
    }

    @Test
    public void testGetInstances() {
        List<ServiceInstance> instances = serviceDiscovery.getInstances("user-service");
        assertEquals(2, instances.size());
    }

    @Test
    public void testChooseInstance() {
        ServiceInstance instance = serviceDiscovery.chooseInstance("user-service");
        assertNotNull(instance);
        assertTrue(instance.getInstanceId().equals("instance-1") || instance.getInstanceId().equals("instance-2"));
    }

    @Test
    public void testRefreshCache() {
        serviceDiscovery.refreshCache();
        
        List<ServiceInstance> instances = serviceDiscovery.getInstances("user-service");
        assertEquals(2, instances.size());
    }

    @Test
    public void testChooseInstanceForNonExistentService() {
        ServiceInstance instance = serviceDiscovery.chooseInstance("non-existent-service");
        assertNull(instance);
    }
}
