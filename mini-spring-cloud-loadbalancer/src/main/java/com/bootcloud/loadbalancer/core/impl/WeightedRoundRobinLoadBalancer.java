package com.bootcloud.loadbalancer.core.impl;

import com.bootcloud.loadbalancer.core.LoadBalancer;
import com.bootcloud.registry.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    private static final Logger log = LoggerFactory.getLogger(WeightedRoundRobinLoadBalancer.class);
    
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            log.warn("No available instances");
            return null;
        }

        int totalWeight = instances.stream()
                .mapToInt(instance -> getWeight(instance))
                .sum();
        
        if (totalWeight == 0) {
            int currentIndex = index.getAndIncrement() % instances.size();
            return instances.get(currentIndex);
        }

        int current = Math.abs(index.getAndIncrement()) % totalWeight;
        int sum = 0;
        
        for (ServiceInstance instance : instances) {
            sum += getWeight(instance);
            if (current < sum) {
                log.debug("Chosen instance by WeightedRoundRobin: weight={}, instance={}", 
                        getWeight(instance), instance.getInstanceId());
                return instance;
            }
        }
        
        return instances.get(instances.size() - 1);
    }

    @Override
    public String getStrategyName() {
        return "WeightedRoundRobin";
    }

    private int getWeight(ServiceInstance instance) {
        String weight = instance.getMetadata().get("weight");
        return weight != null ? Integer.parseInt(weight) : 1;
    }
}
