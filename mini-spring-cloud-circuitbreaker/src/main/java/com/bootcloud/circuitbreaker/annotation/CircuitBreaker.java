package com.bootcloud.circuitbreaker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {
    String name() default "";
    int failureThreshold() default 5;
    long timeout() default 60000;
    long resetTimeout() default 60000;
    String fallbackMethod() default "";
}
