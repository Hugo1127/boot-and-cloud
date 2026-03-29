package com.bootcloud.core.factory;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.PostConstruct;
import com.bootcloud.core.annotation.PreDestroy;
import com.bootcloud.core.bean.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultListableBeanFactory implements BeanFactory {
    private static final Logger logger = LoggerFactory.getLogger(DefaultListableBeanFactory.class);

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
    private final Map<String, Boolean> singletonsCurrentlyInCreation = new ConcurrentHashMap<>();

    @Override
    public Object getBean(String name) {
        return doGetBean(name, Object.class);
    }

    @Override
    public <T> T getBean(Class<T> type) {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getBeanClass())) {
                return (T) doGetBean(entry.getKey(), type);
            }
        }
        throw new RuntimeException("No bean of type " + type.getName() + " found");
    }

    @Override
    public <T> T getBean(String name, Class<T> type) {
        return doGetBean(name, type);
    }

    @Override
    public boolean containsBean(String name) {
        return beanDefinitionMap.containsKey(name);
    }

    @Override
    public boolean isSingleton(String name) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
        return beanDefinition != null && beanDefinition.isSingleton();
    }

    @Override
    public BeanDefinition getBeanDefinition(String name) {
        return beanDefinitionMap.get(name);
    }

    public void registerBeanDefinition(String name, BeanDefinition beanDefinition) {
        beanDefinitionMap.put(name, beanDefinition);
        logger.debug("Registered bean definition: {} -> {}", name, beanDefinition.getBeanClass().getName());
    }

    private <T> T doGetBean(String name, Class<T> type) {
        Object sharedInstance = getSingleton(name);
        if (sharedInstance != null) {
            return (T) sharedInstance;
        }

        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
        if (beanDefinition == null) {
            throw new RuntimeException("Bean definition not found: " + name);
        }

        if (beanDefinition.isSingleton()) {
            return (T) getSingleton(name, () -> createBean(name, beanDefinition));
        } else {
            return (T) createBean(name, beanDefinition);
        }
    }

    private Object getSingleton(String name) {
        Object singletonObject = singletonObjects.get(name);
        if (singletonObject == null) {
            singletonObject = earlySingletonObjects.get(name);
            if (singletonObject == null) {
                ObjectFactory<?> singletonFactory = singletonFactories.get(name);
                if (singletonFactory != null) {
                    singletonObject = singletonFactory.getObject();
                    earlySingletonObjects.put(name, singletonObject);
                    singletonFactories.remove(name);
                }
            }
        }
        return singletonObject;
    }

    private Object getSingleton(String name, ObjectFactory<?> objectFactory) {
        if (singletonsCurrentlyInCreation.containsKey(name)) {
            throw new RuntimeException("Currently in creation: " + name);
        }

        singletonsCurrentlyInCreation.put(name, true);
        try {
            Object singletonObject = objectFactory.getObject();
            singletonObjects.put(name, singletonObject);
            earlySingletonObjects.remove(name);
            singletonFactories.remove(name);
            return singletonObject;
        } finally {
            singletonsCurrentlyInCreation.remove(name);
        }
    }

    private Object createBean(String name, BeanDefinition beanDefinition) {
        try {
            Object bean = doCreateBean(name, beanDefinition);

            for (Method method : beanDefinition.getPreDestroyMethods()) {
                method.setAccessible(true);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        method.invoke(bean);
                        logger.debug("Executed @PreDestroy method: {} on bean: {}", method.getName(), name);
                    } catch (Exception e) {
                        logger.error("Failed to execute @PreDestroy method", e);
                    }
                }));
            }

            return bean;
        } catch (Exception e) {
            logger.error("Failed to create bean: " + name, e);
            throw new RuntimeException("Failed to create bean: " + name, e);
        }
    }

    private Object doCreateBean(String name, BeanDefinition beanDefinition) throws Exception {
        Object instance = beanDefinition.getInstance();
        if (instance == null) {
            instance = createInstance(beanDefinition);
            beanDefinition.setInstance(instance);
        }

        if (beanDefinition.isSingleton()) {
            if (singletonsCurrentlyInCreation.containsKey(name)) {
                singletonFactories.put(name, () -> getEarlyBeanReference(name, beanDefinition, instance));
            }
        }

        populateBean(beanDefinition, instance);

        initializeBean(beanDefinition, instance);

        return instance;
    }

    private Object getEarlyBeanReference(String name, BeanDefinition beanDefinition, Object bean) {
        return bean;
    }

    private Object createInstance(BeanDefinition beanDefinition) throws Exception {
        Class<?> beanClass = beanDefinition.getBeanClass();
        Constructor<?> constructor = beanDefinition.getConstructor();

        if (constructor != null) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                args[i] = getBean(parameterTypes[i]);
            }
            return constructor.newInstance(args);
        }

        return beanClass.getDeclaredConstructor().newInstance();
    }

    private void populateBean(BeanDefinition beanDefinition, Object bean) throws Exception {
        for (Field field : beanDefinition.getAutowiredFields()) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            Class<?> fieldType = field.getType();

            Object dependency;
            if (field.isAnnotationPresent(Autowired.class)) {
                dependency = getBean(fieldType);
            } else {
                dependency = getBean(field.getName(), fieldType);
            }

            field.setAccessible(true);
            field.set(bean, dependency);
            logger.debug("Autowired field: {} in bean: {}", field.getName(), beanDefinition.getBeanName());
        }
    }

    private void initializeBean(BeanDefinition beanDefinition, Object bean) throws Exception {
        for (Method method : beanDefinition.getPostConstructMethods()) {
            method.setAccessible(true);
            method.invoke(bean);
            logger.debug("Executed @PostConstruct method: {} on bean: {}", method.getName(), beanDefinition.getBeanName());
        }
    }

    public void preInstantiateSingletons() {
        for (String beanName : beanDefinitionMap.keySet()) {
            if (isSingleton(beanName)) {
                getBean(beanName);
            }
        }
    }

    public void close() {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            BeanDefinition beanDefinition = entry.getValue();
            Object bean = singletonObjects.get(entry.getKey());
            if (bean != null) {
                for (Method method : beanDefinition.getPreDestroyMethods()) {
                    try {
                        method.setAccessible(true);
                        method.invoke(bean);
                        logger.debug("Executed @PreDestroy method: {} on bean: {}", method.getName(), entry.getKey());
                    } catch (Exception e) {
                        logger.error("Failed to execute @PreDestroy method", e);
                    }
                }
            }
        }
        singletonObjects.clear();
        earlySingletonObjects.clear();
        singletonFactories.clear();
    }

    @FunctionalInterface
    public interface ObjectFactory<T> {
        T getObject();
    }
}
