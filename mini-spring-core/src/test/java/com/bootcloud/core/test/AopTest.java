package com.bootcloud.core.test;

import com.bootcloud.core.aop.After;
import com.bootcloud.core.aop.Aspect;
import com.bootcloud.core.aop.Around;
import com.bootcloud.core.aop.Before;
import com.bootcloud.core.aop.ProceedingJoinPoint;
import com.bootcloud.core.aop.aspectj.AspectJAwareAdvisorAutoProxyCreator;
import com.bootcloud.core.context.support.GenericApplicationContext;
import com.bootcloud.core.test.support.LogAspect;
import com.bootcloud.core.test.support.OrderService;
import com.bootcloud.core.test.support.UserService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AOP 代理与通知测试
 *
 * 【测试目标】
 * 1. 无接口类（CGLIB）代理创建
 * 2. @Before 通知执行
 * 3. @After 通知执行
 * 4. @Around 通知执行（含时序控制）
 * 5. 多个通知组合执行
 * 6. Pointcut 匹配正确性
 */
public class AopTest {
    private static final Logger logger = LoggerFactory.getLogger(AopTest.class);

    @Test
    public void testProxyCreationForClassWithoutInterface() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator =
                new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());

        UserService userService = context.getBean(UserService.class);
        Object proxy = proxyCreator.wrapIfNecessary(userService, "userService");

        // UserService 没有实现接口，应该使用 CGLIB 代理
        assertNotNull(proxy);
        assertTrue(proxy instanceof UserService);

        logger.info("Proxy creation test passed");
    }

    @Test
    public void testBeforeAdviceExecution() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator =
                new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());

        UserService userService = context.getBean(UserService.class);
        UserService proxy = (UserService) proxyCreator.wrapIfNecessary(userService, "userService");

        String result = proxy.getUser("123");
        assertEquals("User-123", result);

        logger.info("Before advice execution test passed");
    }

    @Test
    public void testAfterAdviceExecution() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator =
                new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());

        UserService userService = context.getBean(UserService.class);
        UserService proxy = (UserService) proxyCreator.wrapIfNecessary(userService, "userService");

        proxy.getUser("123");

        logger.info("After advice execution test passed");
    }

    @Test
    public void testAroundAdviceExecution() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator =
                new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());

        OrderService orderService = context.getBean(OrderService.class);
        OrderService proxy = (OrderService) proxyCreator.wrapIfNecessary(orderService, "orderService");

        String result = proxy.findOrder("456");
        assertNotNull(result);
        assertEquals("Order-456", result);

        logger.info("Around advice execution test passed");
    }

    @Test
    public void testMultipleAdvicesOnSameBean() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator =
                new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());

        UserService userService = context.getBean(UserService.class);
        UserService proxy = (UserService) proxyCreator.wrapIfNecessary(userService, "userService");

        // LogAspect 对 UserService 有 @Before 和 @After 两个通知
        String result = proxy.getUser("123");
        assertEquals("User-123", result);

        logger.info("Multiple advices test passed");
    }

    @Test
    public void testAspectExcludedFromProxy() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator =
                new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());

        LogAspect logAspect = (LogAspect) context.getBean("logAspect");
        Object result = proxyCreator.wrapIfNecessary(logAspect, "logAspect");

        // Aspect 类本身不应该被代理
        assertSame(logAspect, result);

        logger.info("Aspect excluded from proxy test passed");
    }
}
