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
 * 1. 验证单例 Bean 在并发环境下的线程安全性
 * 2. 验证原型 Bean 每次都创建新实例
 * 3. 验证双重检查锁定（DCL）的正确性
 * 
 * 【面试考点】
 * 1. 为什么 ConcurrentHashMap 不能保证并发安全？
 *    - 只能保证单个 put/get 操作原子，不能保证"查询 + 创建"复合操作原子
 * 2. 为什么需要 ReentrantLock？
 *    - 确保"查询 + 创建"的原子性，防止多线程重复创建
 * 3. 双重检查锁定的作用？
 *    - 第一次检查（无锁）：避免不必要的锁竞争，提升性能
 *    - 第二次检查（有锁）：防止多线程同时通过第一次检查后重复创建
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

        assertSame(bean1, bean2, "单例 Bean 应该是同一个对象");
        assertSame(bean2, bean3, "单例 Bean 应该是同一个对象");
        assertEquals(bean1.getId(), bean2.getId(), "单例 Bean 的 ID 应该相同");

        logger.info("Singleton scope test passed - all beans are the same instance");
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

        assertNotSame(bean1, bean2, "原型 Bean 应该是不同的对象");
        assertNotSame(bean2, bean3, "原型 Bean 应该是不同的对象");
        assertNotEquals(bean1.getId(), bean2.getId(), "原型 Bean 的 ID 应该不同");

        logger.info("Prototype scope test passed - all beans are different instances");
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
                    SingletonBean bean = context.getBean(SingletonBean.class);
                    beans.add(bean);
                    latch.countDown();
                } catch (Exception e) {
                    logger.error("Failed to get bean", e);
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount, beans.size(), "所有线程都应该成功获取 Bean");

        Set<SingletonBean> uniqueBeans = new HashSet<>(beans);
        assertEquals(1, uniqueBeans.size(), "所有线程获取的应该是同一个单例 Bean");

        logger.info("Concurrency test passed - {} threads got the same singleton instance", threadCount);
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
                    PrototypeBean bean = context.getBean(PrototypeBean.class);
                    beans.add(bean);
                    latch.countDown();
                } catch (Exception e) {
                    logger.error("Failed to get bean", e);
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount, beans.size(), "所有线程都应该成功获取 Bean");

        Set<String> uniqueIds = new HashSet<>();
        for (PrototypeBean bean : beans) {
            uniqueIds.add(bean.getId());
        }

        assertEquals(threadCount, uniqueIds.size(), "每个线程获取的应该是不同的原型 Bean");

        logger.info("Prototype concurrency test passed - {} threads got {} different instances", 
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
        AtomicInteger creationCount = new AtomicInteger(0);
        List<SingletonBean> beans = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    SingletonBean bean = context.getBean(SingletonBean.class);
                    beans.add(bean);
                    creationCount.incrementAndGet();
                    latch.countDown();
                } catch (Exception e) {
                    logger.error("Failed to get bean", e);
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount, beans.size(), "所有线程都应该成功获取 Bean");

        Set<SingletonBean> uniqueBeans = new HashSet<>(beans);
        assertEquals(1, uniqueBeans.size(), "双重检查锁定应该确保只有一个 Bean 实例");

        logger.info("DCL test passed - {} threads, but only 1 instance created", threadCount);
        context.close();
    }

    @Test
    public void testBeanScopeDefinition() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        assertTrue(context.isSingleton("singletonBean"), "SingletonBean 应该是单例");
        assertFalse(context.isSingleton("prototypeBean"), "PrototypeBean 不应该是单例");

        assertEquals("singleton", context.getBeanDefinition("singletonBean").getScope());
        assertEquals("prototype", context.getBeanDefinition("prototypeBean").getScope());

        logger.info("Bean scope definition test passed");
        context.close();
    }
}
