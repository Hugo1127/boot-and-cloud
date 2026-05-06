package com.bootcloud.boot;

import com.bootcloud.boot.context.BootApplicationContext;
import com.bootcloud.boot.web.server.WebServerLifecycle;
import com.bootcloud.core.context.ApplicationContext;
import com.bootcloud.core.context.support.GenericApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringApplication {
    private static final Logger logger = LoggerFactory.getLogger(SpringApplication.class);

    private static BootApplicationContext applicationContext;

    private final Class<?> primarySource;
    private final BootApplicationContext context;

    public SpringApplication(Class<?> primarySource) {
        this.primarySource = primarySource;
        this.context = new BootApplicationContext();
    }

    public static SpringApplication run(Class<?> primarySource, String... args) {
        SpringApplication app = new SpringApplication(primarySource);
        app.run(args);
        return app;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public ApplicationContext run(String... args) {
        try {
            logger.info("Starting Boot&Cloud application...");
            logger.info("Primary source: {}", primarySource.getName());

            context.scan(getBasePackage());
            
            context.loadAutoConfiguration();
            
            context.refresh();

            applicationContext = context;

            startWebServers(context);

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

    private void startWebServers(BootApplicationContext context) {
        try {
            WebServerLifecycle lifecycle = new WebServerLifecycle(context);
            lifecycle.start();
        } catch (Exception e) {
            logger.warn("Failed to start web servers: {}", e.getMessage());
        }
    }

    public BootApplicationContext getContext() {
        return context;
    }
}
