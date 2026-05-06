package com.bootcloud.boot.web.server;

import com.bootcloud.core.context.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 在应用上下文刷新后自动启动所有 WebServer bean。
 * 类似 Spring Boot 的 SmartLifecycle 机制。
 */
public class WebServerLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(WebServerLifecycle.class);

    private final ApplicationContext context;

    public WebServerLifecycle(ApplicationContext context) {
        this.context = context;
    }

    /**
     * 查找并启动所有 WebServer 类型的 bean。
     * 每个 WebServer 的 start() 方法将在独立线程中启动，避免阻塞主线程。
     */
    public void start() {
        Map<String, WebServer> webServers = context.getBeansOfType(WebServer.class);
        if (webServers.isEmpty()) {
            logger.info("No WebServer beans found, skipping web server startup");
            return;
        }

        for (Map.Entry<String, WebServer> entry : webServers.entrySet()) {
            String beanName = entry.getKey();
            WebServer webServer = entry.getValue();
            logger.info("Starting WebServer bean: {} on port {}", beanName, webServer.getPort());
            startServer(webServer);
        }
    }

    private void startServer(WebServer webServer) {
        Thread serverThread = new Thread(() -> {
            try {
                webServer.start();
            } catch (Exception e) {
                LoggerFactory.getLogger(WebServerLifecycle.class)
                        .error("Failed to start WebServer: {}", e.getMessage(), e);
            }
        }, "web-server-lifecycle");
        serverThread.setDaemon(false);
        serverThread.start();
    }
}