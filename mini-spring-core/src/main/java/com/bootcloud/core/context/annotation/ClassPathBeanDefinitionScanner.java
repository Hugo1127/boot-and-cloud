package com.bootcloud.core.context.annotation;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.Component;
import com.bootcloud.core.annotation.Controller;
import com.bootcloud.core.annotation.PostConstruct;
import com.bootcloud.core.annotation.PreDestroy;
import com.bootcloud.core.annotation.Repository;
import com.bootcloud.core.annotation.Service;
import com.bootcloud.core.bean.BeanDefinition;
import com.bootcloud.core.factory.DefaultListableBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassPathBeanDefinitionScanner {
    private static final Logger logger = LoggerFactory.getLogger(ClassPathBeanDefinitionScanner.class);

    private final DefaultListableBeanFactory beanFactory;

    public ClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void scan(String basePackage) {
        logger.info("Scanning base package: {}", basePackage);
        String packagePath = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());
                scanDirectory(directory, basePackage);
            }
        } catch (IOException e) {
            logger.error("Failed to scan package: " + basePackage, e);
        }

        logger.info("Scan completed. Total bean definitions: {}", beanFactory.getBeanDefinitionMap().size());
    }

    private void scanDirectory(File directory, String packageName) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    registerBeanIfAnnotated(clazz);
                } catch (ClassNotFoundException e) {
                    logger.warn("Class not found: " + className, e);
                }
            }
        }
    }

    private void registerBeanIfAnnotated(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Component.class) ||
            clazz.isAnnotationPresent(Service.class) ||
            clazz.isAnnotationPresent(Repository.class) ||
            clazz.isAnnotationPresent(Controller.class)) {

            String beanName = generateBeanName(clazz);
            BeanDefinition beanDefinition = new BeanDefinition(clazz);
            beanDefinition.setBeanName(beanName);

            parseBeanDefinition(clazz, beanDefinition);

            beanFactory.registerBeanDefinition(beanName, beanDefinition);
            logger.info("Registered bean: {} -> {}", beanName, clazz.getName());
        }
    }

    private String generateBeanName(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }

        Service service = clazz.getAnnotation(Service.class);
        if (service != null && !service.value().isEmpty()) {
            return service.value();
        }

        Repository repository = clazz.getAnnotation(Repository.class);
        if (repository != null && !repository.value().isEmpty()) {
            return repository.value();
        }

        Controller controller = clazz.getAnnotation(Controller.class);
        if (controller != null && !controller.value().isEmpty()) {
            return controller.value();
        }

        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private void parseBeanDefinition(Class<?> clazz, BeanDefinition beanDefinition) {
        parseAutowiredFields(clazz, beanDefinition);
        parseConstructors(clazz, beanDefinition);
        parseLifecycleMethods(clazz, beanDefinition);
    }

    private void parseAutowiredFields(Class<?> clazz, BeanDefinition beanDefinition) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    beanDefinition.getAutowiredFields().add(field);
                    logger.debug("Found @Autowired field: {} in class: {}", field.getName(), currentClass.getName());
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    private void parseConstructors(Class<?> clazz, BeanDefinition beanDefinition) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                beanDefinition.setConstructor(constructor);
                logger.debug("Found @Autowired constructor in class: {}", clazz.getName());
                break;
            }
        }
    }

    private void parseLifecycleMethods(Class<?> clazz, BeanDefinition beanDefinition) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    beanDefinition.getPostConstructMethods().add(method);
                    logger.debug("Found @PostConstruct method: {} in class: {}", method.getName(), currentClass.getName());
                }
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    beanDefinition.getPreDestroyMethods().add(method);
                    logger.debug("Found @PreDestroy method: {} in class: {}", method.getName(), currentClass.getName());
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }
}
