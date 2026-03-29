package com.bootcloud.loadbalancer.core.impl;

import com.bootcloud.loadbalancer.core.LoadBalancer;
import com.bootcloud.registry.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LeastActiveLoadBalancer implements LoadBalancer {
    private static final Logger log = LoggerFactory.getLogger(LeastActiveLoadBalancer.class);
    
    private final ConcurrentHashMap<String, AtomicInteger> activeCountMap = new ConcurrentHashMap<>();

    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            log.warn("No available instances");
            return null;
        }

        ServiceInstance chosen = instances.stream()
                .min((i1, i2) -> {
                    int count1 = getActiveCount(i1.getInstanceId());
                    int count2 = getActiveCount(i2.getInstanceId());
                    return Integer.compare(count1, count2);
                })
                .orElse(instances.get(0));

        incrementActiveCount(chosen.getInstanceId());
        
        log.debug("Chosen instance by LeastActive: active={}, instance={}", 
                getActiveCount(chosen.getInstanceId()), chosen.getInstanceId());
        return chosen;
    }

    @Override
    public String getStrategyName() {
        return "LeastActive";
    }

    public void completeRequest(String instanceId) {
        decrementActiveCount(instanceId);
    }

    private int getActiveCount(String instanceId) {
        return activeCountMap.computeIfAbsent(instanceId, k -> new AtomicInteger(0)).get();
    }

    private void incrementActiveCount(String instanceId) {
        activeCountMap.computeIfAbsent(instanceId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private void decrementActiveCount(String instanceId) {
        AtomicInteger count = activeCountMap.get(instanceId);
        if (count != null) {
            count.decrementAndGet();
        }
    }
}
