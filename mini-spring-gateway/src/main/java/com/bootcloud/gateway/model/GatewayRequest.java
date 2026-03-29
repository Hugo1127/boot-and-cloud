package com.bootcloud.gateway.model;

import java.util.HashMap;
import java.util.Map;

public class GatewayRequest {
    private String path;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> params;
    private String body;
    private Object attributes;

    public GatewayRequest() {
        this.headers = new HashMap<>();
        this.params = new HashMap<>();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Object getAttributes() {
        return attributes;
    }

    public void setAttributes(Object attributes) {
        this.attributes = attributes;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public void addParam(String key, String value) {
        this.params.put(key, value);
    }

    @Override
    public String toString() {
        return "GatewayRequest{" +
                "path='" + path + '\'' +
                ", method='" + method + '\'' +
                ", headers=" + headers +
                ", params=" + params +
                '}';
    }
}
