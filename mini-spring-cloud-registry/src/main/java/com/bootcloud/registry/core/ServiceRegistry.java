package com.bootcloud.registry.core;

import com.bootcloud.registry.model.ServiceInstance;

public interface ServiceRegistry {
    void register(ServiceInstance instance);

    void deregister(ServiceInstance instance);

    void renew(ServiceInstance instance);

    ServiceInstance getInstance(String serviceId, String instanceId);

    java.util.List<ServiceInstance> getInstances(String serviceId);

    java.util.List<String> getServices();

    void shutdown();
}
