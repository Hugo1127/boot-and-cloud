package com.bootcloud.registry.core.impl;

import com.bootcloud.registry.model.ServiceInstance;
import com.bootcloud.registry.core.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InMemoryServiceRegistry implements ServiceRegistry {
    private static final Logger log = LoggerFactory.getLogger(InMemoryServiceRegistry.class);

    private final Map<String, Set<ServiceInstance>> serviceStore;
    private final Map<String, ServiceInstance> instanceStore;
    private final ScheduledExecutorService healthCheckExecutor;
    private final long heartbeatTimeoutSeconds;

    public InMemoryServiceRegistry(long heartbeatTimeoutSeconds) {
        this.serviceStore = new ConcurrentHashMap<>();
        this.instanceStore = new ConcurrentHashMap<>();
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        this.healthCheckExecutor = Executors.newSingleThreadScheduledExecutor();
        startHealthCheck();
    }

    @Override
    public void register(ServiceInstance instance) {
        log.info("Registering service instance: {}", instance);
        
        String serviceId = instance.getServiceId();
        String instanceId = instance.getInstanceId();
        
        serviceStore.computeIfAbsent(serviceId, k -> ConcurrentHashMap.newKeySet()).add(instance);
        instanceStore.put(instanceId, instance);
        
        log.info("Service registered successfully: {} -> {}", serviceId, instance.getServiceUrl());
    }

    @Override
    public void deregister(ServiceInstance instance) {
        log.info("Deregistering service instance: {}", instance);
        
        String serviceId = instance.getServiceId();
        String instanceId = instance.getInstanceId();
        
        Set<ServiceInstance> instances = serviceStore.get(serviceId);
        if (instances != null) {
            instances.remove(instance);
            if (instances.isEmpty()) {
                serviceStore.remove(serviceId);
            }
        }
        instanceStore.remove(instanceId);
        
        log.info("Service deregistered successfully: {}", instanceId);
    }

    @Override
    public void renew(ServiceInstance instance) {
        log.debug("Renewing service instance: {}", instance.getInstanceId());
        
        ServiceInstance storedInstance = instanceStore.get(instance.getInstanceId());
        if (storedInstance != null) {
            storedInstance.updateHeartbeat();
            log.debug("Heartbeat renewed for instance: {}", instance.getInstanceId());
        }
    }

    @Override
    public ServiceInstance getInstance(String serviceId, String instanceId) {
        return instanceStore.get(instanceId);
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        Set<ServiceInstance> instances = serviceStore.get(serviceId);
        if (instances == null) {
            return Collections.emptyList();
        }
        return instances.stream()
                .filter(ServiceInstance::isHealthy)
                .toList();
    }

    @Override
    public List<String> getServices() {
        return new ArrayList<>(serviceStore.keySet());
    }

    @Override
    public void shutdown() {
        log.info("Shutting down service registry");
        healthCheckExecutor.shutdown();
        try {
            if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void startHealthCheck() {
        healthCheckExecutor.scheduleAtFixedRate(() -> {
            try {
                performHealthCheck();
            } catch (Exception e) {
                log.error("Error during health check", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void performHealthCheck() {
        log.debug("Performing health check for all instances");
        
        List<ServiceInstance> expiredInstances = new ArrayList<>();
        for (ServiceInstance instance : instanceStore.values()) {
            if (instance.isExpired(heartbeatTimeoutSeconds)) {
                log.warn("Instance expired: {}", instance.getInstanceId());
                instance.setHealthy(false);
                expiredInstances.add(instance);
            }
        }
        
        for (ServiceInstance instance : expiredInstances) {
            deregister(instance);
        }
        
        log.info("Health check completed. Total services: {}, Total instances: {}", 
                serviceStore.size(), instanceStore.size());
    }
}
