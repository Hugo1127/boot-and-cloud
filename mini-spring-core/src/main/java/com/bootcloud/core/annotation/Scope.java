package com.bootcloud.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于指定 Bean 的作用域
 * 支持单例（singleton）和原型（prototype）两种作用域
 * 
 * 面试考点：
 * 1. singleton：整个容器中只有一个实例，线程安全问题需要通过加锁解决
 * 2. prototype：每次获取都创建新实例，不存在线程安全问题
 * 3. 默认作用域为 singleton
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {
    
    /**
     * 作用域类型
     * 默认值为 singleton（单例）
     */
    String value() default "singleton";
    
    /**
     * 单例作用域常量
     */
    String SCOPE_SINGLETON = "singleton";
    
    /**
     * 原型作用域常量（多例）
     */
    String SCOPE_PROTOTYPE = "prototype";
}
