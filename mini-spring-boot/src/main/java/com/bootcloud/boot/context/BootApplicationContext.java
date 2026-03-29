package com.bootcloud.boot.context;

import com.bootcloud.boot.condition.OnClassCondition;
import com.bootcloud.boot.condition.OnPropertyCondition;
import com.bootcloud.core.context.support.GenericApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

public class BootApplicationContext extends GenericApplicationContext {
    private static final Logger logger = LoggerFactory.getLogger(BootApplicationContext.class);

    private final Environment environment;

    public BootApplicationContext() {
        this.environment = new Environment();
        logger.info("Boot application context created");
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void loadAutoConfiguration() {
        logger.info("Loading auto-configuration classes");
        AutoConfigurationLoader loader = new AutoConfigurationLoader();
        loader.loadAutoConfigurationClasses(this);
        logger.info("Auto-configuration loaded");
    }

    public static class Environment {
        private final Properties properties;

        public Environment() {
            this.properties = new Properties();
            loadDefaultProperties();
        }

        private void loadDefaultProperties() {
            properties.setProperty("server.port", "8080");
            properties.setProperty("server.host", "0.0.0.0");
            properties.setProperty("spring.application.name", "bootcloud-application");
        }

        public String getProperty(String key) {
            return properties.getProperty(key);
        }

        public String getProperty(String key, String defaultValue) {
            return properties.getProperty(key, defaultValue);
        }

        public void setProperty(String key, String value) {
            properties.setProperty(key, value);
        }

        public Properties getProperties() {
            return properties;
        }
    }
}
