package com.bootcloud.feign.core;

import com.bootcloud.feign.annotation.FeignClient;
import com.bootcloud.feign.client.FeignInvocationHandler;
import com.bootcloud.registry.core.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feign 客户端工厂
 * 用于创建 Feign 接口的代理实例
 * 支持服务发现集成
 * 
 * 面试考点：
 * 1. Feign 客户端是如何创建的？
 *    答：基于 JDK 动态代理，为接口生成代理实例
 * 2. Feign 的核心组件有哪些？
 *    答：Encoder（编码器）、Decoder（解码器）、Client（HTTP 客户端）
 * 3. Feign 如何集成服务发现？
 *    答：通过 FeignInvocationHandler 传入 ServiceDiscovery 实例
 */
public class FeignClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(FeignClientFactory.class);
    
    private static final Map<Class<?>, Object> clientCache = new ConcurrentHashMap<>();
    private static ServiceDiscovery serviceDiscovery;
    
    /**
     * 设置服务发现实例（全局）
     */
    public static void setServiceDiscovery(ServiceDiscovery discovery) {
        serviceDiscovery = discovery;
        logger.info("FeignClientFactory configured with ServiceDiscovery");
    }
    
    /**
     * 创建 Feign 客户端代理（使用服务发现）
     * @param clazz Feign 客户端接口
     * @param <T> 接口类型
     * @return 代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> clazz) {
        return create(clazz, serviceDiscovery);
    }
    
    /**
     * 创建 Feign 客户端代理（指定服务发现）
     * @param clazz Feign 客户端接口
     * @param discovery 服务发现实例
     * @param <T> 接口类型
     * @return 代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> clazz, ServiceDiscovery discovery) {
        return (T) clientCache.computeIfAbsent(clazz, cls -> {
            FeignClient feignClient = cls.getAnnotation(FeignClient.class);
            if (feignClient == null) {
                throw new IllegalArgumentException("Class " + cls.getName() + " is not annotated with @FeignClient");
            }
            
            logger.info("Creating Feign client for interface: {}", cls.getName());
            logger.info("Service name: {}, URL: {}", feignClient.name(), feignClient.url());
            
            // 创建代理实例
            return Proxy.newProxyInstance(
                cls.getClassLoader(),
                new Class<?>[] {cls},
                new FeignInvocationHandler(feignClient, discovery)
            );
        });
    }
    
    /**
     * 清除缓存
     */
    public static void clearCache() {
        clientCache.clear();
        logger.info("Feign client cache cleared");
    }
}
