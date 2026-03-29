package com.bootcloud.core.aop.aspectj;

import com.bootcloud.core.aop.*;
import com.bootcloud.core.bean.BeanDefinition;
import com.bootcloud.core.factory.BeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AspectJAwareAdvisorAutoProxyCreator {
    private static final Logger logger = LoggerFactory.getLogger(AspectJAwareAdvisorAutoProxyCreator.class);

    private final BeanFactory beanFactory;
    private final List<Object> aspects = new ArrayList<>();

    public AspectJAwareAdvisorAutoProxyCreator(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        scanAspects();
    }

    private void scanAspects() {
        for (String beanName : beanFactory.getBeanDefinitionMap().keySet()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            Class<?> beanClass = beanDefinition.getBeanClass();
            if (beanClass.isAnnotationPresent(Aspect.class)) {
                try {
                    Object aspect = beanFactory.getBean(beanName);
                    aspects.add(aspect);
                    logger.debug("Found aspect: {}", beanName);
                } catch (Exception e) {
                    logger.error("Failed to get aspect bean: " + beanName, e);
                }
            }
        }
    }

    public Object wrapIfNecessary(Object bean, String beanName) {
        if (shouldSkip(bean.getClass(), beanName)) {
            return bean;
        }

        ProxyFactory proxyFactory = new ProxyFactory(bean);
        List<Advice> advices = getAdvices(bean.getClass());
        proxyFactory.getAdvices().addAll(advices);

        if (!advices.isEmpty()) {
            logger.debug("Creating proxy for bean: {} with {} advices", beanName, advices.size());
            return proxyFactory.getProxy();
        }

        return bean;
    }

    private boolean shouldSkip(Class<?> beanClass, String beanName) {
        if (beanClass.isAnnotationPresent(Aspect.class)) {
            return true;
        }
        return false;
    }

    private List<Advice> getAdvices(Class<?> targetClass) {
        List<Advice> advices = new ArrayList<>();

        for (Object aspect : aspects) {
            Method[] methods = aspect.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Before.class)) {
                    Before before = method.getAnnotation(Before.class);
                    advices.add(new BeforeAdvice(aspect, method, before.value()));
                    logger.debug("Found @Before advice: {} in aspect: {}", method.getName(), aspect.getClass().getName());
                } else if (method.isAnnotationPresent(After.class)) {
                    After after = method.getAnnotation(After.class);
                    advices.add(new AfterAdvice(aspect, method, after.value()));
                    logger.debug("Found @After advice: {} in aspect: {}", method.getName(), aspect.getClass().getName());
                } else if (method.isAnnotationPresent(Around.class)) {
                    Around around = method.getAnnotation(Around.class);
                    advices.add(new AroundAdvice(aspect, method, around.value()));
                    logger.debug("Found @Around advice: {} in aspect: {}", method.getName(), aspect.getClass().getName());
                }
            }
        }

        return advices;
    }

    public List<Object> getAspects() {
        return aspects;
    }
}
