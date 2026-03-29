package com.bootcloud.feign.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式 HTTP 客户端注解
 * 用于标记 Feign 客户端接口，自动生成 HTTP 请求实现
 * 
 * 面试考点：
 * 1. Feign 的核心原理是什么？
 *    答：基于动态代理，在运行时为接口生成实现类，将方法调用转换为 HTTP 请求
 * 2. Feign 与 Ribbon 如何集成？
 *    答：Feign 默认集成 Ribbon，实现客户端负载均衡
 * 3. Feign 的优缺点？
 *    答：优点是声明式、接口化、简单易用；缺点是性能略低于直接 HTTP 调用
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignClient {
    /**
     * 服务名称（用于服务发现）
     */
    String name() default "";
    
    /**
     * 服务 URL（直接指定地址，不使用服务发现）
     */
    String url() default "";
    
    /**
     * 路径前缀
     */
    String path() default "";
    
    /**
     * 解码器
     */
    Class<?> decoder() default Object.class;
    
    /**
     * 编码器
     */
    Class<?> encoder() default Object.class;
}
