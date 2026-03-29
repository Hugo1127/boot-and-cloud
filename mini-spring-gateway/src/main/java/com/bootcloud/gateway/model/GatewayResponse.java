package com.bootcloud.gateway.model;

import java.util.HashMap;
import java.util.Map;

public class GatewayResponse {
    private int status;
    private Map<String, String> headers;
    private String body;
    private boolean filtered;

    public GatewayResponse() {
        this.status = 200;
        this.headers = new HashMap<>();
        this.filtered = false;
    }

    public GatewayResponse(int status, String body) {
        this();
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public static GatewayResponse ok(String body) {
        return new GatewayResponse(200, body);
    }

    public static GatewayResponse notFound() {
        return new GatewayResponse(404, "Not Found");
    }

    public static GatewayResponse internalError(String message) {
        return new GatewayResponse(500, "Internal Server Error: " + message);
    }

    public static GatewayResponse tooManyRequests(String message) {
        return new GatewayResponse(429, message);
    }

    @Override
    public String toString() {
        return "GatewayResponse{" +
                "status=" + status +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                ", filtered=" + filtered +
                '}';
    }
}
