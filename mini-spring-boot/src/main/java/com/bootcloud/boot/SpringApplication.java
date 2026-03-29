package com.bootcloud.boot;

import com.bootcloud.boot.context.BootApplicationContext;
import com.bootcloud.core.context.ApplicationContext;
import com.bootcloud.core.context.support.GenericApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringApplication {
    private static final Logger logger = LoggerFactory.getLogger(SpringApplication.class);

    private final Class<?> primarySource;
    private final BootApplicationContext context;

    public SpringApplication(Class<?> primarySource) {
        this.primarySource = primarySource;
        this.context = new BootApplicationContext();
    }

    public static SpringApplication run(Class<?> primarySource, String... args) {
        return new SpringApplication(primarySource).run(args);
    }

    public ApplicationContext run(String... args) {
        try {
            logger.info("Starting Boot&Cloud application...");
            logger.info("Primary source: {}", primarySource.getName());

            context.scan(getBasePackage());
            
            context.loadAutoConfiguration();
            
            context.refresh();

            logger.info("Boot&Cloud application started successfully");
            logger.info("Application context ID: {}", context.getId());
            logger.info("Startup time: {}ms", System.currentTimeMillis() - context.getStartupDate());

            return context;
        } catch (Exception e) {
            logger.error("Failed to start Boot&Cloud application", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }

    private String getBasePackage() {
        return primarySource.getPackage().getName();
    }

    public BootApplicationContext getContext() {
        return context;
    }
}
