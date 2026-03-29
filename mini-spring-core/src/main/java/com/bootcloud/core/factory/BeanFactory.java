package com.bootcloud.core.factory;

import com.bootcloud.core.bean.BeanDefinition;

public interface BeanFactory {
    Object getBean(String name);

    <T> T getBean(Class<T> type);

    <T> T getBean(String name, Class<T> type);

    boolean containsBean(String name);

    boolean isSingleton(String name);

    BeanDefinition getBeanDefinition(String name);
}
