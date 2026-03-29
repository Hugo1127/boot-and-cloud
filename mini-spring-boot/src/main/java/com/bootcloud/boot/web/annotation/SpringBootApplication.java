package com.bootcloud.boot.web.annotation;

import com.bootcloud.boot.annotation.EnableAutoConfiguration;
import com.bootcloud.core.annotation.ComponentScan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableAutoConfiguration
@ComponentScan
public @interface SpringBootApplication {
    String[] scanBasePackages() default {};
}
