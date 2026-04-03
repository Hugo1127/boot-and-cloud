package com.bootcloud.feign.client;

import com.bootcloud.feign.annotation.FeignClient;
import com.bootcloud.feign.annotation.RequestMapping;
import com.bootcloud.feign.annotation.PathVariable;
import com.bootcloud.feign.annotation.RequestBody;
import com.bootcloud.feign.codec.Encoder;
import com.bootcloud.feign.codec.Decoder;
import com.bootcloud.feign.codec.JsonEncoder;
import com.bootcloud.feign.codec.JsonDecoder;
import com.bootcloud.loadbalancer.core.LoadBalancer;
import com.bootcloud.loadbalancer.factory.LoadBalancerFactory;
import com.bootcloud.registry.core.ServiceDiscovery;
import com.bootcloud.registry.model.ServiceInstance;
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
import java.util.List;
import java.util.Map;

/**
 * Feign 调用处理器
 * 拦截接口方法调用，转换为 HTTP 请求
 * 集成服务发现与负载均衡能力
 * 
 * 面试考点：
 * 1. 动态代理的核心原理？
 *    答：通过 InvocationHandler 拦截方法调用，在运行时增强或转换行为
 * 2. Feign 如何与服务注册中心集成？
 *    答：通过 ServiceDiscovery 获取服务实例列表，结合 LoadBalancer 选择实例
 * 3. 服务名如何解析为实际地址？
 *    答：从 ServiceDiscovery 缓存中获取服务实例，拼接 host:port 生成 URL
 */
public class FeignInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(FeignInvocationHandler.class);
    
    private final FeignClient feignClient;
    private final HttpClient httpClient;
    private final Encoder encoder;
    private final Decoder decoder;
    private final ServiceDiscovery serviceDiscovery;
    private final LoadBalancer loadBalancer;
    private final String fixedUrl;
    private final boolean useServiceDiscovery;
    
    public FeignInvocationHandler(FeignClient feignClient, ServiceDiscovery serviceDiscovery) {
        this.feignClient = feignClient;
        this.httpClient = HttpClient.newHttpClient();
        this.encoder = new JsonEncoder();
        this.decoder = new JsonDecoder();
        this.serviceDiscovery = serviceDiscovery;
        this.loadBalancer = LoadBalancerFactory.createLoadBalancer("roundRobin");
        
        // 确定基础 URL
        if (!feignClient.url().isEmpty()) {
            this.fixedUrl = feignClient.url();
            this.useServiceDiscovery = false;
            logger.info("Feign client '{}' configured with fixed URL: {}", feignClient.name(), fixedUrl);
        } else if (!feignClient.name().isEmpty()) {
            this.fixedUrl = null;
            this.useServiceDiscovery = true;
            logger.info("Feign client '{}' configured with service discovery for service: {}", 
                       feignClient.name(), feignClient.name());
        } else {
            throw new IllegalArgumentException("FeignClient must specify either 'name' or 'url'");
        }
    }
    
    /**
     * 兼容旧版本构造函数（不使用服务发现）
     */
    public FeignInvocationHandler(FeignClient feignClient) {
        this(feignClient, null);
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
        
        // 构建完整 URL（使用服务发现或固定 URL）
        String url = buildUrl(path);
        
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
     * 构建完整的请求 URL
     * 支持服务发现和固定 URL 两种模式
     * 
     * 面试考点：
     * 1. 服务名如何转换为实际 URL？
     *    答：从 ServiceDiscovery 获取服务实例，拼接 http://host:port + path
     * 2. 负载均衡在何时生效？
     *    答：在 chooseInstance() 时，通过 LoadBalancer 从多个实例中选择一个
     */
    private String buildUrl(String path) {
        if (useServiceDiscovery) {
            // 使用服务发现：从注册中心获取服务实例
            String serviceName = feignClient.name();
            ServiceInstance instance = chooseInstance(serviceName);
            
            if (instance == null) {
                throw new RuntimeException("No available instance found for service: " + serviceName);
            }
            
            String baseUrl = "http://" + instance.getHost() + ":" + instance.getPort();
            String url = baseUrl + path;
            
            logger.debug("Service discovery resolved '{}' to instance {}:{}", 
                        serviceName, instance.getHost(), instance.getPort());
            
            return url;
        } else {
            // 使用固定 URL
            return fixedUrl + path;
        }
    }
    
    /**
     * 选择服务实例（集成负载均衡）
     * 
     * 面试考点：
     * 1. 负载均衡策略有哪些？
     *    答：轮询、随机、权重、最少活跃数等
     * 2. 本项目默认使用什么策略？
     *    答：轮询策略（RoundRobin），保证请求均匀分布
     */
    private ServiceInstance chooseInstance(String serviceName) {
        if (serviceDiscovery == null) {
            logger.error("ServiceDiscovery is not configured!");
            throw new RuntimeException("ServiceDiscovery not available for service: " + serviceName);
        }
        
        // 从服务发现获取实例列表
        List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
        
        if (instances == null || instances.isEmpty()) {
            logger.error("No instances found for service: {}", serviceName);
            return null;
        }
        
        // 过滤健康实例
        List<ServiceInstance> healthyInstances = instances.stream()
                .filter(ServiceInstance::isHealthy)
                .toList();
        
        if (healthyInstances.isEmpty()) {
            logger.error("No healthy instances found for service: {}", serviceName);
            return null;
        }
        
        // 使用负载均衡器选择实例
        ServiceInstance chosen = loadBalancer.choose(healthyInstances);
        logger.debug("LoadBalancer chose instance {}:{} for service {}", 
                    chosen.getHost(), chosen.getPort(), serviceName);
        
        return chosen;
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
