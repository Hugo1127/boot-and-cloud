package com.bootcloud.core.aop;

import java.lang.reflect.Method;

public class ProceedingJoinPoint {
    private final Object target;
    private final Method method;
    private final Object[] args;

    public ProceedingJoinPoint(Object target, Method method, Object[] args) {
        this.target = target;
        this.method = method;
        this.args = args;
    }

    public Object getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getSignature() {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    public Object proceed() throws Throwable {
        return method.invoke(target, args);
    }

    public Object proceed(Object[] args) throws Throwable {
        return method.invoke(target, args);
    }
}
