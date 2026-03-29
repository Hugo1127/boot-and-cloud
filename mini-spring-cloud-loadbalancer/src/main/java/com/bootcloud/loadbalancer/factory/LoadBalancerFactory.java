package com.bootcloud.loadbalancer.factory;

import com.bootcloud.loadbalancer.core.LoadBalancer;
import com.bootcloud.loadbalancer.core.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadBalancerFactory {
    private static final Logger log = LoggerFactory.getLogger(LoadBalancerFactory.class);

    public static LoadBalancer createLoadBalancer(String strategy) {
        if (strategy == null || strategy.isEmpty()) {
            log.debug("No strategy specified, using default RoundRobin");
            return new RoundRobinLoadBalancer();
        }

        switch (strategy.toLowerCase()) {
            case "roundrobin":
                log.debug("Creating RoundRobinLoadBalancer");
                return new RoundRobinLoadBalancer();
            case "random":
                log.debug("Creating RandomLoadBalancer");
                return new RandomLoadBalancer();
            case "weighted":
                log.debug("Creating WeightedRoundRobinLoadBalancer");
                return new WeightedRoundRobinLoadBalancer();
            case "leastactive":
                log.debug("Creating LeastActiveLoadBalancer");
                return new LeastActiveLoadBalancer();
            default:
                log.warn("Unknown strategy: {}, using default RoundRobin", strategy);
                return new RoundRobinLoadBalancer();
        }
    }
}
