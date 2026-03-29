package com.bootcloud.registry.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableServiceRegistry {
    String serverAddress() default "localhost:8081";
    int heartbeatInterval() default 30;
    int heartbeatTimeout() default 90;
}
