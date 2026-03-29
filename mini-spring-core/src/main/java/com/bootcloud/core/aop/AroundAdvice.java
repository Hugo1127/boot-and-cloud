package com.bootcloud.core.aop;

import java.lang.reflect.Method;

public class AroundAdvice implements Advice {
    private final Object aspect;
    private final Method adviceMethod;
    private final PointcutExpression pointcutExpression;

    public AroundAdvice(Object aspect, Method adviceMethod, String pointcutExpression) {
        this.aspect = aspect;
        this.adviceMethod = adviceMethod;
        this.pointcutExpression = new PointcutExpression(pointcutExpression);
    }

    public Object around(Object target, Method method, Object[] args) throws Throwable {
        if (pointcutExpression.matches(target.getClass(), method)) {
            adviceMethod.setAccessible(true);
            return adviceMethod.invoke(aspect, new ProceedingJoinPoint(target, method, args));
        }
        return method.invoke(target, args);
    }

    public PointcutExpression getPointcutExpression() {
        return pointcutExpression;
    }
}
