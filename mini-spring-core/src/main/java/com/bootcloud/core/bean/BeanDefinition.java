package com.bootcloud.core.bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class BeanDefinition {
    private String beanName;
    private Class<?> beanClass;
    private Object instance;
    private Constructor<?> constructor;
    private List<Field> autowiredFields;
    private List<Method> autowiredMethods;
    private List<Method> postConstructMethods;
    private List<Method> preDestroyMethods;
    private boolean singleton;
    private boolean lazyInit;
    private String scope;

    public BeanDefinition(Class<?> beanClass) {
        this.beanClass = beanClass;
        this.singleton = true;
        this.lazyInit = false;
        this.scope = "singleton";
        this.autowiredFields = new ArrayList<>();
        this.autowiredMethods = new ArrayList<>();
        this.postConstructMethods = new ArrayList<>();
        this.preDestroyMethods = new ArrayList<>();
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public void setConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public List<Field> getAutowiredFields() {
        return autowiredFields;
    }

    public List<Method> getAutowiredMethods() {
        return autowiredMethods;
    }

    public List<Method> getPostConstructMethods() {
        return postConstructMethods;
    }

    public List<Method> getPreDestroyMethods() {
        return preDestroyMethods;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public boolean isLazyInit() {
        return lazyInit;
    }

    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
