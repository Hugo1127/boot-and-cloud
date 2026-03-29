package com.bootcloud.core.aop;

import java.lang.reflect.Method;

public class BeforeAdvice implements Advice {
    private final Object aspect;
    private final Method adviceMethod;
    private final PointcutExpression pointcutExpression;

    public BeforeAdvice(Object aspect, Method adviceMethod, String pointcutExpression) {
        this.aspect = aspect;
        this.adviceMethod = adviceMethod;
        this.pointcutExpression = new PointcutExpression(pointcutExpression);
    }

    public void before(Object target, Method method, Object[] args) throws Throwable {
        if (pointcutExpression.matches(target.getClass(), method)) {
            adviceMethod.setAccessible(true);
            adviceMethod.invoke(aspect, new ProceedingJoinPoint(target, method, args));
        }
    }

    public PointcutExpression getPointcutExpression() {
        return pointcutExpression;
    }
}
