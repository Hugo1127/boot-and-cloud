package com.bootcloud.registry.test;

import com.bootcloud.registry.core.ServiceRegistry;
import com.bootcloud.registry.core.impl.InMemoryServiceRegistry;
import com.bootcloud.registry.model.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceRegistryTest {
    private ServiceRegistry serviceRegistry;

    @BeforeEach
    public void setUp() {
        serviceRegistry = new InMemoryServiceRegistry(90);
    }

    @AfterEach
    public void tearDown() {
        serviceRegistry.shutdown();
    }

    @Test
    public void testRegisterService() {
        ServiceInstance instance = new ServiceInstance("user-service", "instance-1", "localhost", 8080);
        serviceRegistry.register(instance);

        List<ServiceInstance> instances = serviceRegistry.getInstances("user-service");
        assertEquals(1, instances.size());
        assertEquals("instance-1", instances.get(0).getInstanceId());
    }

    @Test
    public void testRegisterMultipleInstances() {
        ServiceInstance instance1 = new ServiceInstance("user-service", "instance-1", "localhost", 8080);
        ServiceInstance instance2 = new ServiceInstance("user-service", "instance-2", "localhost", 8081);
        
        serviceRegistry.register(instance1);
        serviceRegistry.register(instance2);

        List<ServiceInstance> instances = serviceRegistry.getInstances("user-service");
        assertEquals(2, instances.size());
    }

    @Test
    public void testDeregisterService() {
        ServiceInstance instance = new ServiceInstance("user-service", "instance-1", "localhost", 8080);
        serviceRegistry.register(instance);
        
        assertEquals(1, serviceRegistry.getInstances("user-service").size());
        
        serviceRegistry.deregister(instance);
        assertEquals(0, serviceRegistry.getInstances("user-service").size());
    }

    @Test
    public void testRenewHeartbeat() {
        ServiceInstance instance = new ServiceInstance("user-service", "instance-1", "localhost", 8080);
        serviceRegistry.register(instance);
        
        assertTrue(instance.isHealthy());
        
        serviceRegistry.renew(instance);
        assertTrue(instance.isHealthy());
    }

    @Test
    public void testGetServices() {
        ServiceInstance instance1 = new ServiceInstance("user-service", "instance-1", "localhost", 8080);
        ServiceInstance instance2 = new ServiceInstance("order-service", "instance-2", "localhost", 8081);
        
        serviceRegistry.register(instance1);
        serviceRegistry.register(instance2);

        List<String> services = serviceRegistry.getServices();
        assertEquals(2, services.size());
        assertTrue(services.contains("user-service"));
        assertTrue(services.contains("order-service"));
    }

    @Test
    public void testGetInstance() {
        ServiceInstance instance = new ServiceInstance("user-service", "instance-1", "localhost", 8080);
        serviceRegistry.register(instance);

        ServiceInstance found = serviceRegistry.getInstance("user-service", "instance-1");
        assertNotNull(found);
        assertEquals("instance-1", found.getInstanceId());
    }

    @Test
    public void testServiceUrl() {
        ServiceInstance instance = new ServiceInstance("user-service", "instance-1", "192.168.1.100", 8080);
        assertEquals("192.168.1.100:8080", instance.getServiceUrl());
    }
}
