package com.bootcloud.core.factory;

import com.bootcloud.core.bean.BeanDefinition;

import java.util.Map;

public interface BeanFactory {
    Object getBean(String name);

    <T> T getBean(Class<T> type);

    <T> T getBean(String name, Class<T> type);

    boolean containsBean(String name);

    boolean isSingleton(String name);

    BeanDefinition getBeanDefinition(String name);

    Map<String, BeanDefinition> getBeanDefinitionMap();

    <T> Map<String, T> getBeansOfType(Class<T> type);
}
