package com.bootcloud.registry.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServiceInstance {
    private String serviceId;
    private String instanceId;
    private String host;
    private int port;
    private boolean healthy;
    private LocalDateTime registerTime;
    private LocalDateTime lastHeartbeatTime;
    private Map<String, String> metadata;

    public ServiceInstance() {
        this.registerTime = LocalDateTime.now();
        this.lastHeartbeatTime = LocalDateTime.now();
        this.healthy = true;
        this.metadata = new HashMap<>();
    }

    public ServiceInstance(String serviceId, String instanceId, String host, int port) {
        this();
        this.serviceId = serviceId;
        this.instanceId = instanceId;
        this.host = host;
        this.port = port;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }

    public LocalDateTime getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(LocalDateTime lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getServiceUrl() {
        return host + ":" + port;
    }

    public void updateHeartbeat() {
        this.lastHeartbeatTime = LocalDateTime.now();
        this.healthy = true;
    }

    public boolean isExpired(long timeoutSeconds) {
        return lastHeartbeatTime.plusSeconds(timeoutSeconds).isBefore(LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return Objects.equals(instanceId, that.instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId);
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "serviceId='" + serviceId + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", healthy=" + healthy +
                ", registerTime=" + registerTime +
                ", lastHeartbeatTime=" + lastHeartbeatTime +
                ", metadata=" + metadata +
                '}';
    }
}
