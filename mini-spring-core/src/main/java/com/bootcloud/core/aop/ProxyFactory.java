package com.bootcloud.core.aop;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class ProxyFactory {
    private static final Logger logger = LoggerFactory.getLogger(ProxyFactory.class);

    private final Object target;
    private final List<Advice> advices;

    public ProxyFactory(Object target) {
        this.target = target;
        this.advices = new ArrayList<>();
    }

    public void addAdvice(Advice advice) {
        advices.add(advice);
    }

    public List<Advice> getAdvices() {
        return new ArrayList<>(advices);
    }

    public Object getProxy() {
        Class<?> targetClass = target.getClass();

        if (targetClass.getInterfaces().length > 0) {
            return createJdkProxy();
        } else {
            return createCglibProxy();
        }
    }

    private Object createJdkProxy() {
        logger.debug("Creating JDK dynamic proxy for class: {}", target.getClass().getName());
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            new JdkInvocationHandler(target, advices)
        );
    }

    private Object createCglibProxy() {
        logger.debug("Creating CGLIB proxy for class: {}", target.getClass().getName());
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target.getClass());
        enhancer.setCallback(new CglibMethodInterceptor(target, advices));
        return enhancer.create();
    }

    private static class JdkInvocationHandler implements InvocationHandler {
        private final Object target;
        private final List<Advice> advices;

        public JdkInvocationHandler(Object target, List<Advice> advices) {
            this.target = target;
            this.advices = advices;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            for (Advice advice : advices) {
                if (advice instanceof BeforeAdvice) {
                    ((BeforeAdvice) advice).before(target, method, args);
                }
            }

            Object result = null;
            boolean aroundExecuted = false;

            for (Advice advice : advices) {
                if (advice instanceof AroundAdvice) {
                    result = ((AroundAdvice) advice).around(target, method, args);
                    aroundExecuted = true;
                }
            }

            if (!aroundExecuted) {
                result = method.invoke(target, args);
            }

            for (Advice advice : advices) {
                if (advice instanceof AfterAdvice) {
                    ((AfterAdvice) advice).after(target, method, args);
                }
            }

            return result;
        }
    }

    private static class CglibMethodInterceptor implements MethodInterceptor {
        private final Object target;
        private final List<Advice> advices;

        public CglibMethodInterceptor(Object target, List<Advice> advices) {
            this.target = target;
            this.advices = advices;
        }

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            for (Advice advice : advices) {
                if (advice instanceof BeforeAdvice) {
                    ((BeforeAdvice) advice).before(target, method, args);
                }
            }

            Object result = null;
            boolean aroundExecuted = false;

            for (Advice advice : advices) {
                if (advice instanceof AroundAdvice) {
                    result = ((AroundAdvice) advice).around(target, method, args);
                    aroundExecuted = true;
                }
            }

            if (!aroundExecuted) {
                result = proxy.invoke(target, args);
            }

            for (Advice advice : advices) {
                if (advice instanceof AfterAdvice) {
                    ((AfterAdvice) advice).after(target, method, args);
                }
            }

            return result;
        }
    }
}
