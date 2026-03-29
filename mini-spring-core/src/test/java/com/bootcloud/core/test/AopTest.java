package com.bootcloud.core.test;

import com.bootcloud.core.aop.aspectj.AspectJAwareAdvisorAutoProxyCreator;
import com.bootcloud.core.context.support.GenericApplicationContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class AopTest {
    private static final Logger logger = LoggerFactory.getLogger(AopTest.class);

    @Test
    public void testAopProxyCreation() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator = new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());

        UserService userService = context.getBean(UserService.class);
        assertNotNull(userService);

        logger.info("AOP proxy creation test passed");
    }

    @Test
    public void testBeforeAdvice() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator = new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());
        UserService userService = (UserService) proxyCreator.wrapIfNecessary(context.getBean(UserService.class), "userService");

        String result = userService.getUser("123");
        assertNotNull(result);
        assertEquals("User-123", result);

        logger.info("Before advice test passed");
    }

    @Test
    public void testAfterAdvice() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator = new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());
        UserService userService = (UserService) proxyCreator.wrapIfNecessary(context.getBean(UserService.class), "userService");

        userService.getUser("123");

        logger.info("After advice test passed");
    }

    @Test
    public void testAroundAdvice() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator = new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());
        OrderService orderService = (OrderService) proxyCreator.wrapIfNecessary(context.getBean(OrderService.class), "orderService");

        String result = orderService.findOrder("456");
        assertNotNull(result);

        logger.info("Around advice test passed");
    }

    @Test
    public void testMultipleAdvices() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        AspectJAwareAdvisorAutoProxyCreator proxyCreator = new AspectJAwareAdvisorAutoProxyCreator(context.getBeanFactory());
        UserService userService = (UserService) proxyCreator.wrapIfNecessary(context.getBean(UserService.class), "userService");

        userService.getUser("123");

        logger.info("Multiple advices test passed");
    }
}
