package com.bootcloud.registry.core.impl;

import com.bootcloud.registry.core.ServiceDiscovery;
import com.bootcloud.registry.core.ServiceRegistry;
import com.bootcloud.registry.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class DefaultServiceDiscovery implements ServiceDiscovery {
    private static final Logger log = LoggerFactory.getLogger(DefaultServiceDiscovery.class);

    private final ServiceRegistry serviceRegistry;
    private final Map<String, List<ServiceInstance>> serviceCache;
    private final ScheduledExecutorService refreshExecutor;
    private final long cacheRefreshIntervalSeconds;

    public DefaultServiceDiscovery(ServiceRegistry serviceRegistry, long cacheRefreshIntervalSeconds) {
        this.serviceRegistry = serviceRegistry;
        this.serviceCache = new ConcurrentHashMap<>();
        this.cacheRefreshIntervalSeconds = cacheRefreshIntervalSeconds;
        this.refreshExecutor = Executors.newSingleThreadScheduledExecutor();
        startCacheRefresh();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        List<ServiceInstance> cachedInstances = serviceCache.get(serviceId);
        if (cachedInstances != null) {
            log.debug("Returning cached instances for service: {}", serviceId);
            return cachedInstances;
        }
        
        refreshCache(serviceId);
        return serviceCache.getOrDefault(serviceId, Collections.emptyList());
    }

    @Override
    public ServiceInstance chooseInstance(String serviceId) {
        List<ServiceInstance> instances = getInstances(serviceId);
        if (instances.isEmpty()) {
            log.warn("No available instances for service: {}", serviceId);
            return null;
        }
        
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        ServiceInstance chosen = instances.get(index);
        log.debug("Chosen instance for service {}: {}", serviceId, chosen.getInstanceId());
        return chosen;
    }

    @Override
    public void refreshCache() {
        log.debug("Refreshing service discovery cache");
        
        List<String> serviceIds = serviceRegistry.getServices();
        for (String serviceId : serviceIds) {
            refreshCache(serviceId);
        }
        
        log.info("Cache refreshed. Services: {}", serviceCache.size());
    }

    private void refreshCache(String serviceId) {
        List<ServiceInstance> instances = serviceRegistry.getInstances(serviceId);
        serviceCache.put(serviceId, instances);
        log.debug("Cache refreshed for service: {}, instances: {}", serviceId, instances.size());
    }

    private void startCacheRefresh() {
        refreshExecutor.scheduleAtFixedRate(() -> {
            try {
                refreshCache();
            } catch (Exception e) {
                log.error("Error during cache refresh", e);
            }
        }, cacheRefreshIntervalSeconds, cacheRefreshIntervalSeconds, TimeUnit.SECONDS);
    }

    public void shutdown() {
        log.info("Shutting down service discovery");
        refreshExecutor.shutdown();
        try {
            if (!refreshExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                refreshExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            refreshExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
