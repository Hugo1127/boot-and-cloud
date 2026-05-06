package com.bootcloud.boot.context;

import com.bootcloud.boot.annotation.ConditionalOnClass;
import com.bootcloud.boot.annotation.ConditionalOnProperty;
import com.bootcloud.boot.condition.OnClassCondition;
import com.bootcloud.boot.condition.OnPropertyCondition;
import com.bootcloud.core.bean.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AutoConfigurationLoader {
    private static final Logger logger = LoggerFactory.getLogger(AutoConfigurationLoader.class);

    private final OnClassCondition onClassCondition = new OnClassCondition();
    private final OnPropertyCondition onPropertyCondition = new OnPropertyCondition();

    public void loadAutoConfigurationClasses(BootApplicationContext context) {
        List<String> configurationClasses = loadConfigurationClasses();
        for (String className : configurationClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                if (shouldLoadConfiguration(clazz, context)) {
                    loadConfiguration(clazz, context);
                    logger.info("Loaded auto-configuration class: {}", className);
                } else {
                    logger.debug("Skipped auto-configuration class: {} (condition not met)", className);
                }
            } catch (Exception e) {
                logger.error("Failed to load auto-configuration class: " + className, e);
            }
        }
    }

    private List<String> loadConfigurationClasses() {
        List<String> classes = new ArrayList<>();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("META-INF/spring.factories");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    classes.add(line);
                }
            }
        } catch (Exception e) {
            logger.warn("No spring.factories file found or error reading it", e);
        }
        return classes;
    }

    private boolean shouldLoadConfiguration(Class<?> clazz, BootApplicationContext context) {
        if (clazz.isAnnotationPresent(ConditionalOnClass.class)) {
            ConditionalOnClass condition = clazz.getAnnotation(ConditionalOnClass.class);
            if (!onClassCondition.matches(context, condition.value())) {
                return false;
            }
        }

        if (clazz.isAnnotationPresent(ConditionalOnProperty.class)) {
            ConditionalOnProperty condition = clazz.getAnnotation(ConditionalOnProperty.class);
            if (!onPropertyCondition.matches(context, condition.name(), condition.value(), 
                    condition.havingValue(), condition.matchIfMissing())) {
                return false;
            }
        }

        return true;
    }

    private void loadConfiguration(Class<?> clazz, BootApplicationContext context) throws Exception {
        Object configInstance = clazz.getDeclaredConstructor().newInstance();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterCount() == 0 || canResolveParameters(method, context)) {
                if (shouldLoadMethod(method, context)) {
                    Object bean = invokeMethod(method, configInstance, context);
                    String beanName = method.getName();

                    if (context.containsBean(beanName)) {
                        logger.debug("Bean already exists: {}", beanName);
                        continue;
                    }

                    BeanDefinition beanDefinition = new BeanDefinition(bean.getClass());
                    beanDefinition.setBeanName(beanName);
                    beanDefinition.setInstance(bean);
                    context.getBeanFactory().registerBeanDefinition(beanName, beanDefinition);
                    logger.debug("Registered bean from configuration: {}", beanName);
                }
            }
        }
    }

    private boolean canResolveParameters(Method method, BootApplicationContext context) {
        Class<?>[] paramTypes = method.getParameterTypes();
        for (Class<?> paramType : paramTypes) {
            if (!BootApplicationContext.class.isAssignableFrom(paramType)) {
                return false;
            }
        }
        return true;
    }

    private Object invokeMethod(Method method, Object target, BootApplicationContext context) throws Exception {
        method.setAccessible(true);
        if (method.getParameterCount() == 0) {
            return method.invoke(target);
        }
        Object[] args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            Class<?> paramType = method.getParameterTypes()[i];
            if (BootApplicationContext.class.isAssignableFrom(paramType)) {
                args[i] = context;
            } else {
                throw new IllegalStateException("Cannot resolve parameter of type " + paramType.getName()
                        + " for method " + method.getName());
            }
        }
        return method.invoke(target, args);
    }

    private boolean shouldLoadMethod(Method method, BootApplicationContext context) {
        if (method.isAnnotationPresent(ConditionalOnClass.class)) {
            ConditionalOnClass condition = method.getAnnotation(ConditionalOnClass.class);
            if (!onClassCondition.matches(context, condition.value())) {
                return false;
            }
        }

        if (method.isAnnotationPresent(ConditionalOnProperty.class)) {
            ConditionalOnProperty condition = method.getAnnotation(ConditionalOnProperty.class);
            if (!onPropertyCondition.matches(context, condition.name(), condition.value(), 
                    condition.havingValue(), condition.matchIfMissing())) {
                return false;
            }
        }

        return true;
    }
}
