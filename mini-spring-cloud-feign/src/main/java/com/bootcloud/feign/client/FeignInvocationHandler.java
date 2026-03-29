package com.bootcloud.feign.client;

import com.bootcloud.feign.annotation.FeignClient;
import com.bootcloud.feign.annotation.RequestMapping;
import com.bootcloud.feign.annotation.PathVariable;
import com.bootcloud.feign.annotation.RequestBody;
import com.bootcloud.feign.codec.Encoder;
import com.bootcloud.feign.codec.Decoder;
import com.bootcloud.feign.codec.JsonEncoder;
import com.bootcloud.feign.codec.JsonDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Feign 调用处理器
 * 拦截接口方法调用，转换为 HTTP 请求
 * 
 * 面试考点：
 * 1. 动态代理的核心原理？
 *    答：通过 InvocationHandler 拦截方法调用，在运行时增强或转换行为
 */
public class FeignInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(FeignInvocationHandler.class);
    
    private final FeignClient feignClient;
    private final HttpClient httpClient;
    private final Encoder encoder;
    private final Decoder decoder;
    private final String baseUrl;
    
    public FeignInvocationHandler(FeignClient feignClient) {
        this.feignClient = feignClient;
        this.httpClient = HttpClient.newHttpClient();
        this.encoder = new JsonEncoder();
        this.decoder = new JsonDecoder();
        
        // 确定基础 URL
        if (!feignClient.url().isEmpty()) {
            this.baseUrl = feignClient.url();
        } else {
            // TODO: 从服务注册中心获取服务地址
            this.baseUrl = "http://localhost:8080";
            logger.warn("Service name '{}' specified but service discovery not implemented yet. Using default URL: {}", 
                       feignClient.name(), this.baseUrl);
        }
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Object 类的方法直接调用
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        
        // 检查是否有 RequestMapping 注解
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            throw new IllegalStateException("Method " + method.getName() + " must have @RequestMapping annotation");
        }
        
        // 构建请求
        String httpMethod = requestMapping.method();
        String path = requestMapping.path();
        
        // 处理路径变量
        Map<String, String> pathVariables = extractPathVariables(method, args);
        path = replacePathVariables(path, pathVariables);
        
        // 构建完整 URL
        String url = baseUrl + path;
        
        // 获取请求体
        Object requestBody = extractRequestBody(method, args);
        
        // 创建 HTTP 请求
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json");
        
        // 设置请求方法和体
        if ("GET".equals(httpMethod)) {
            requestBuilder.GET();
        } else if ("POST".equals(httpMethod)) {
            if (requestBody != null) {
                String bodyStr = encoder.encode(requestBody);
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyStr));
            } else {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            }
        } else if ("PUT".equals(httpMethod)) {
            if (requestBody != null) {
                String bodyStr = encoder.encode(requestBody);
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(bodyStr));
            } else {
                requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
            }
        } else if ("DELETE".equals(httpMethod)) {
            requestBuilder.DELETE();
        } else {
            throw new UnsupportedOperationException("HTTP method " + httpMethod + " not supported");
        }
        
        HttpRequest httpRequest = requestBuilder.build();
        
        logger.debug("Sending {} request to {}", httpMethod, url);
        
        // 发送请求并获取响应
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        logger.debug("Received response: status={}, body={}", response.statusCode(), response.body());
        
        // 处理响应
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            if (method.getReturnType() == void.class || method.getReturnType() == Void.class) {
                return null;
            }
            return decoder.decode(response.body(), method.getReturnType());
        } else {
            throw new RuntimeException("HTTP request failed with status: " + response.statusCode());
        }
    }
    
    /**
     * 提取路径变量
     */
    private Map<String, String> extractPathVariables(Method method, Object[] args) {
        Map<String, String> pathVariables = new HashMap<>();
        if (args == null) return pathVariables;
        
        for (int i = 0; i < args.length; i++) {
            PathVariable pathVariable = method.getParameters()[i].getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String name = pathVariable.value().isEmpty() ? method.getParameters()[i].getName() : pathVariable.value();
                pathVariables.put(name, args[i].toString());
            }
        }
        return pathVariables;
    }
    
    /**
     * 替换路径中的变量
     */
    private String replacePathVariables(String path, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            path = path.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return path;
    }
    
    /**
     * 提取请求体
     */
    private Object extractRequestBody(Method method, Object[] args) {
        if (args == null) return null;
        
        for (int i = 0; i < args.length; i++) {
            RequestBody requestBody = method.getParameters()[i].getAnnotation(RequestBody.class);
            if (requestBody != null) {
                return args[i];
            }
        }
        return null;
    }
}
