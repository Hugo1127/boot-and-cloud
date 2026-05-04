package com.bootcloud.core.test;

import com.bootcloud.core.annotation.Component;
import com.bootcloud.core.annotation.Scope;
import com.bootcloud.core.context.support.GenericApplicationContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bean 作用域和并发安全测试
 *
 * 【测试目标】
 * 1. 单例 Bean 全局唯一
 * 2. 原型 Bean 每次获取都是新实例
 * 3. 50 线程并发获取单例 → 全部是同一个实例
 * 4. 50 线程并发获取原型 → 全部是不同实例
 * 5. 双重检查锁定正确性
 */
public class ScopeAndConcurrencyTest {
    private static final Logger logger = LoggerFactory.getLogger(ScopeAndConcurrencyTest.class);

    @Component
    public static class SingletonBean {
        private final long createTime;
        private final String id;

        public SingletonBean() {
            this.createTime = System.currentTimeMillis();
            this.id = "Singleton-" + System.nanoTime();
        }

        public long getCreateTime() {
            return createTime;
        }

        public String getId() {
            return id;
        }
    }

    @Component
    @Scope(Scope.SCOPE_PROTOTYPE)
    public static class PrototypeBean {
        private final long createTime;
        private final String id;
        private static final AtomicInteger counter = new AtomicInteger(0);

        public PrototypeBean() {
            this.createTime = System.currentTimeMillis();
            this.id = "Prototype-" + counter.incrementAndGet() + "-" + System.nanoTime();
        }

        public long getCreateTime() {
            return createTime;
        }

        public String getId() {
            return id;
        }
    }

    @Test
    public void testSingletonScope() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        SingletonBean bean1 = context.getBean(SingletonBean.class);
        SingletonBean bean2 = context.getBean(SingletonBean.class);
        SingletonBean bean3 = context.getBean("singletonBean", SingletonBean.class);

        assertSame(bean1, bean2, "Singleton beans should be the same instance");
        assertSame(bean2, bean3, "Singleton beans should be the same instance");
        assertEquals(bean1.getId(), bean2.getId());

        logger.info("Singleton scope test passed");
        context.close();
    }

    @Test
    public void testPrototypeScope() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        PrototypeBean bean1 = context.getBean(PrototypeBean.class);
        PrototypeBean bean2 = context.getBean(PrototypeBean.class);
        PrototypeBean bean3 = context.getBean("prototypeBean", PrototypeBean.class);

        assertNotSame(bean1, bean2, "Prototype beans should be different instances");
        assertNotSame(bean2, bean3, "Prototype beans should be different instances");
        assertNotEquals(bean1.getId(), bean2.getId());

        logger.info("Prototype scope test passed");
        context.close();
    }

    @Test
    public void testSingletonConcurrency() throws InterruptedException {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<SingletonBean> beans = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    beans.add(context.getBean(SingletonBean.class));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount, beans.size(), "All threads should get a bean");
        assertEquals(1, new HashSet<>(beans).size(), "All threads should get the same singleton");

        logger.info("Singleton concurrency test passed - {} threads, 1 instance", threadCount);
        context.close();
    }

    @Test
    public void testPrototypeConcurrency() throws InterruptedException {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<PrototypeBean> beans = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    beans.add(context.getBean(PrototypeBean.class));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount, beans.size());
        Set<String> uniqueIds = new HashSet<>();
        for (PrototypeBean bean : beans) {
            uniqueIds.add(bean.getId());
        }
        assertEquals(threadCount, uniqueIds.size(), "Each thread should get a different prototype instance");

        logger.info("Prototype concurrency test passed - {} threads, {} unique instances",
                threadCount, uniqueIds.size());
        context.close();
    }

    @Test
    public void testDoubleCheckedLocking() throws InterruptedException {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<SingletonBean> beans = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    beans.add(context.getBean(SingletonBean.class));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount, beans.size());
        assertEquals(1, new HashSet<>(beans).size(), "DCL should result in exactly one instance");

        logger.info("DCL test passed - {} threads, 1 instance", threadCount);
        context.close();
    }

    @Test
    public void testBeanScopeDefinition() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        assertTrue(context.isSingleton("singletonBean"));
        assertFalse(context.isSingleton("prototypeBean"));

        assertEquals("singleton", context.getBeanDefinition("singletonBean").getScope());
        assertEquals("prototype", context.getBeanDefinition("prototypeBean").getScope());

        logger.info("Bean scope definition test passed");
        context.close();
    }
}
