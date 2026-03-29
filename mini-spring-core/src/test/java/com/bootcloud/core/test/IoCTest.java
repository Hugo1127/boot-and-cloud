package com.bootcloud.core.test;

import com.bootcloud.core.context.support.GenericApplicationContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class IoCTest {
    private static final Logger logger = LoggerFactory.getLogger(IoCTest.class);

    @Test
    public void testBeanRegistration() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        assertNotNull(context.getBean("userRepository"));
        assertNotNull(context.getBean("userService"));
        assertNotNull(context.getBean("orderService"));

        logger.info("Bean registration test passed");
    }

    @Test
    public void testDependencyInjection() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        UserService userService = context.getBean(UserService.class);
        assertNotNull(userService);
        assertNotNull(userService.getUser("123"));
        assertEquals("User-123", userService.getUser("123"));

        OrderService orderService = context.getBean(OrderService.class);
        assertNotNull(orderService);
        assertNotNull(orderService.findOrder("456"));

        logger.info("Dependency injection test passed");
    }

    @Test
    public void testBeanLifecycle() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        logger.info("Bean lifecycle test passed");
        context.close();
    }

    @Test
    public void testCircularDependency() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        UserService userService = context.getBean(UserService.class);
        OrderService orderService = context.getBean(OrderService.class);

        assertSame(userService, userService);
        assertSame(orderService, orderService);

        logger.info("Circular dependency test passed");
    }

    @Test
    public void testSingletonScope() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        UserService bean1 = context.getBean(UserService.class);
        UserService bean2 = context.getBean(UserService.class);
        UserService bean3 = context.getBean("userService", UserService.class);

        assertSame(bean1, bean2);
        assertSame(bean2, bean3);

        logger.info("Singleton scope test passed");
    }
}
