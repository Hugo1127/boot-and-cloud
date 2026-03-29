package com.bootcloud.registry.core;

import com.bootcloud.registry.model.ServiceInstance;

import java.util.List;

public interface ServiceDiscovery {
    List<ServiceInstance> getInstances(String serviceId);

    ServiceInstance chooseInstance(String serviceId);

    void refreshCache();
}
