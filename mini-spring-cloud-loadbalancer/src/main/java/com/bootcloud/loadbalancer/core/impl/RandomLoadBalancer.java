package com.bootcloud.loadbalancer.core.impl;

import com.bootcloud.loadbalancer.core.LoadBalancer;
import com.bootcloud.registry.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancer implements LoadBalancer {
    private static final Logger log = LoggerFactory.getLogger(RandomLoadBalancer.class);

    @Override
    public ServiceInstance choose(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            log.warn("No available instances");
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(instances.size());
        ServiceInstance instance = instances.get(index);
        
        log.debug("Chosen instance by Random: index={}, instance={}", index, instance.getInstanceId());
        return instance;
    }

    @Override
    public String getStrategyName() {
        return "Random";
    }
}
