package com.bootcloud.core.test;

import com.bootcloud.core.annotation.Component;
import com.bootcloud.core.context.support.GenericApplicationContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bean 注册与扫描测试
 *
 * 【测试目标】
 * 1. @Component/@Service/@Repository 注解的类能被扫描到
 * 2. Bean 名称生成规则（首字母小写 / 自定义 value）
 * 3. containsBean / getBeanDefinition 正确性
 */
public class BeanRegistrationTest {
    private static final Logger logger = LoggerFactory.getLogger(BeanRegistrationTest.class);

    @Test
    public void testComponentScanning() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        assertTrue(context.containsBean("userRepository"));
        assertTrue(context.containsBean("userService"));
        assertTrue(context.containsBean("orderService"));

        assertNotNull(context.getBean("userRepository"));
        assertNotNull(context.getBean("userService"));
        assertNotNull(context.getBean("orderService"));

        logger.info("Component scanning test passed");
    }

    @Test
    public void testBeanNameGeneration() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        // 默认名称：类名首字母小写
        assertNotNull(context.getBean("userRepository"));
        assertNotNull(context.getBean("userService"));
        assertNotNull(context.getBean("orderService"));

        logger.info("Bean name generation test passed");
    }

    @Test
    public void testCustomBeanName() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.beans");
        context.refresh();

        // 自定义 Bean 名称
        assertTrue(context.containsBean("customUserRepo"));
        assertNotNull(context.getBean("customUserRepo"));

        logger.info("Custom bean name test passed");
    }

    @Test
    public void testContainsBean() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        assertTrue(context.containsBean("userRepository"));
        assertFalse(context.containsBean("nonExistentBean"));

        logger.info("Contains bean test passed");
    }

    @Test
    public void testBeanDefinition() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test.support");
        context.refresh();

        assertNotNull(context.getBeanDefinition("userRepository"));
        assertEquals("userRepository", context.getBeanDefinition("userRepository").getBeanName());
        assertEquals(com.bootcloud.core.test.support.UserRepository.class,
                context.getBeanDefinition("userRepository").getBeanClass());
        assertTrue(context.isSingleton("userRepository"));

        logger.info("Bean definition test passed");
    }
}
