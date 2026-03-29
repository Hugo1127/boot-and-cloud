package com.bootcloud.gateway.model;

public class Route {
    private String path;
    private String serviceId;
    private String url;
    private int order;
    private boolean enabled;

    public Route() {
        this.enabled = true;
    }

    public Route(String path, String serviceId, String url) {
        this();
        this.path = path;
        this.serviceId = serviceId;
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean matches(String requestPath) {
        return requestPath != null && requestPath.startsWith(path);
    }

    @Override
    public String toString() {
        return "Route{" +
                "path='" + path + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", url='" + url + '\'' +
                ", order=" + order +
                ", enabled=" + enabled +
                '}';
    }
}
