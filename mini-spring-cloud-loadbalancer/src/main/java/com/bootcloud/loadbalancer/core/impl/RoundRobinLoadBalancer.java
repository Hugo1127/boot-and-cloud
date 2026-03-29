package com.bootcloud.loadbalancer.core.impl;

import com.bootcloud.loadbalancer.core.LoadBalancer;
import com.bootcloud.registry.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {
    private static final Logger log = LoggerFactory.getLogger(RoundRobinLoadBalancer.class);
    
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            log.warn("No available instances");
            return null;
        }

        int current = index.getAndIncrement() % instances.size();
        ServiceInstance instance = instances.get(current);
        
        log.debug("Chosen instance by RoundRobin: index={}, instance={}", current, instance.getInstanceId());
        return instance;
    }

    @Override
    public String getStrategyName() {
        return "RoundRobin";
    }
}
