package com.bootcloud.core.test;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.Service;
import com.bootcloud.core.context.support.GenericApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 循环依赖测试
 *
 * 【测试目标】
 * 1. UserService ↔ OrderService 互相依赖 → 三级缓存解决
 * 2. 循环依赖的 Bean 是同一个实例
 * 3. 非循环依赖 Bean 正常创建
 */
public class CircularDependencyTest {

    @Service
    public static class CircularA {
        @Autowired
        private CircularB circularB;

        public CircularB getCircularB() {
            return circularB;
        }

        public String getName() {
            return "CircularA";
        }
    }

    @Service
    public static class CircularB {
        @Autowired
        private CircularA circularA;

        public CircularA getCircularA() {
            return circularA;
        }

        public String getName() {
            return "CircularB";
        }
    }

    @Test
    public void testCircularDependency() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        CircularA a = context.getBean(CircularA.class);
        CircularB b = context.getBean(CircularB.class);

        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(a.getCircularB(), "CircularA should have CircularB injected");
        assertNotNull(b.getCircularA(), "CircularB should have CircularA injected");

        // 互相引用应该指向同一个 Bean 实例
        assertSame(a, b.getCircularA());
        assertSame(b, a.getCircularB());
    }

    @Test
    public void testCircularDependencyWithExistingBeans() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        // 通过名称获取也应该返回正确实例
        CircularA aByName = (CircularA) context.getBean("circularA");
        CircularB bByName = (CircularB) context.getBean("circularB");

        assertSame(aByName, bByName.getCircularA());
        assertSame(bByName, aByName.getCircularB());
    }

    @Test
    public void testNonCircularBeansStillWork() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        // 测试中的 UserService/OrderService 也有循环依赖
        // 验证它们在循环依赖场景下仍能正常工作
        com.bootcloud.core.test.support.UserService userService =
                context.getBean(com.bootcloud.core.test.support.UserService.class);
        com.bootcloud.core.test.support.OrderService orderService =
                context.getBean(com.bootcloud.core.test.support.OrderService.class);

        assertNotNull(userService);
        assertNotNull(orderService);
        assertEquals("User-123", userService.getUser("123"));
        assertEquals("Order-456", orderService.findOrder("456"));
    }
}
