package com.bootcloud.core.context.support;

import com.bootcloud.core.context.ApplicationContext;
import com.bootcloud.core.context.annotation.ClassPathBeanDefinitionScanner;
import com.bootcloud.core.event.ApplicationEvent;
import com.bootcloud.core.event.ApplicationListener;
import com.bootcloud.core.factory.DefaultListableBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GenericApplicationContext implements ApplicationContext {
    private static final Logger logger = LoggerFactory.getLogger(GenericApplicationContext.class);

    private final DefaultListableBeanFactory beanFactory;
    private final List<ApplicationListener<?>> applicationListeners = new ArrayList<>();
    private final Map<Class<?>, List<ApplicationListener<?>>> listenerCache = new ConcurrentHashMap<>();
    private String id;
    private String applicationName;
    private long startupDate;

    public GenericApplicationContext() {
        this.beanFactory = new DefaultListableBeanFactory();
        this.id = "application-" + System.currentTimeMillis();
        this.applicationName = "bootcloud-application";
        this.startupDate = System.currentTimeMillis();
    }

    @Override
    public void refresh() {
        logger.info("Refreshing application context: {}", id);
        startupDate = System.currentTimeMillis();

        logger.info("Pre-instantiating singletons");
        beanFactory.preInstantiateSingletons();

        logger.info("Application context refreshed successfully in {}ms", System.currentTimeMillis() - startupDate);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        logger.debug("Publishing event: {}", event.getClass().getSimpleName());
        for (ApplicationListener<?> listener : applicationListeners) {
            invokeListener(listener, event);
        }
    }

    @SuppressWarnings("unchecked")
    private void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
        try {
            ApplicationListener<ApplicationEvent> applicationListener = (ApplicationListener<ApplicationEvent>) listener;
            applicationListener.onApplicationEvent(event);
        } catch (ClassCastException e) {
            logger.debug("Listener {} not interested in event {}", listener.getClass().getName(), event.getClass().getName());
        }
    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        applicationListeners.add(listener);
        logger.debug("Added application listener: {}", listener.getClass().getName());
    }

    @Override
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public void close() {
        logger.info("Closing application context: {}", id);
        beanFactory.close();
        logger.info("Application context closed successfully");
    }

    public void scan(String basePackage) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
        scanner.scan(basePackage);
    }

    @Override
    public Object getBean(String name) {
        return beanFactory.getBean(name);
    }

    @Override
    public <T> T getBean(Class<T> type) {
        return beanFactory.getBean(type);
    }

    @Override
    public <T> T getBean(String name, Class<T> type) {
        return beanFactory.getBean(name, type);
    }

    @Override
    public boolean containsBean(String name) {
        return beanFactory.containsBean(name);
    }

    @Override
    public boolean isSingleton(String name) {
        return beanFactory.isSingleton(name);
    }

    @Override
    public com.bootcloud.core.bean.BeanDefinition getBeanDefinition(String name) {
        return beanFactory.getBeanDefinition(name);
    }

    public DefaultListableBeanFactory getBeanFactory() {
        return beanFactory;
    }

    public long getStartupDate() {
        return startupDate;
    }
}
