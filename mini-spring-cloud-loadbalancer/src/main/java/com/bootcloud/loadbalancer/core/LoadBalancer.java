package com.bootcloud.loadbalancer.core;

import com.bootcloud.registry.model.ServiceInstance;

import java.util.List;

public interface LoadBalancer {
    ServiceInstance choose(List<ServiceInstance> instances);

    String getStrategyName();
}
