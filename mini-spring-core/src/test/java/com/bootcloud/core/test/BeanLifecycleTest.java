package com.bootcloud.core.test;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.PostConstruct;
import com.bootcloud.core.annotation.PreDestroy;
import com.bootcloud.core.annotation.Service;
import com.bootcloud.core.context.support.GenericApplicationContext;
import com.bootcloud.core.test.support.UserRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bean 生命周期测试
 *
 * 【测试目标】
 * 1. 完整生命周期顺序：实例化 → 依赖注入 → @PostConstruct → 使用 → @PreDestroy
 * 2. @PostConstruct 方法在依赖注入后执行
 * 3. @PreDestroy 在容器关闭时执行
 */
public class BeanLifecycleTest {
    private static final Logger logger = LoggerFactory.getLogger(BeanLifecycleTest.class);

    @Service
    public static class LifecycleBean {
        static final List<String> lifecycleEvents = new ArrayList<>();

        public LifecycleBean() {
            lifecycleEvents.add("constructor");
        }

        @Autowired
        private UserRepository userRepository;

        @PostConstruct
        public void init() {
            lifecycleEvents.add("postConstruct");
            assertNotNull(userRepository, "userRepository should be injected before @PostConstruct");
        }

        @PreDestroy
        public void destroy() {
            lifecycleEvents.add("preDestroy");
        }

        public String getUser(String id) {
            return userRepository.findById(id);
        }

        static void reset() {
            lifecycleEvents.clear();
        }
    }

    @Test
    public void testLifecycleOrder() {
        LifecycleBean.reset();

        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        // 验证执行顺序：constructor → postConstruct
        List<String> events = LifecycleBean.lifecycleEvents;
        assertTrue(events.contains("constructor"));
        assertTrue(events.contains("postConstruct"));
        assertEquals(0, events.indexOf("constructor"), "constructor should execute first");
        assertEquals(1, events.indexOf("postConstruct"), "postConstruct should execute after constructor");

        // Bean 可以正常使用
        LifecycleBean bean = context.getBean(LifecycleBean.class);
        assertNotNull(bean);
        assertEquals("User-999", bean.getUser("999"));

        logger.info("Lifecycle order test passed: {}", events);
    }

    @Test
    public void testPreDestroyOnClose() {
        LifecycleBean.reset();

        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        context.close();

        List<String> events = LifecycleBean.lifecycleEvents;
        assertTrue(events.contains("preDestroy"), "@PreDestroy should execute on context close");

        logger.info("PreDestroy on close test passed: {}", events);
    }

    @Test
    public void testDependencyAvailableInPostConstruct() {
        LifecycleBean.reset();

        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        // 如果 @PostConstruct 中 userRepository 不为 null，则说明依赖注入先于 @PostConstruct 执行
        LifecycleBean bean = context.getBean(LifecycleBean.class);
        assertNotNull(bean.getUser("123"));

        logger.info("Dependency available in @PostConstruct test passed");
    }
}
