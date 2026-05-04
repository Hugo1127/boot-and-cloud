package com.bootcloud.core.test;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.Component;
import com.bootcloud.core.annotation.Service;
import com.bootcloud.core.context.support.GenericApplicationContext;
import com.bootcloud.core.test.support.UserRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Setter 注入专项测试
 *
 * 【测试目标】
 * 1. 单一类型的 Setter 注入
 * 2. 多实现接口 + byName 回退的 Setter 注入
 * 3. Setter 与字段注入在同一 Bean 中共存
 */
public class SetterInjectionTest {
    private static final Logger logger = LoggerFactory.getLogger(SetterInjectionTest.class);

    // --- 单一类型 Setter 注入 ---

    @Service
    public static class SingleSetterBean {
        private UserRepository userRepository;

        @Autowired
        public void setUserRepository(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        public String find(String id) {
            return userRepository.findById(id);
        }
    }

    // --- Setter + 字段混合注入 ---

    @Service
    public static class MixedInjectionBean {
        @Autowired
        private UserRepository userRepository;

        private com.bootcloud.core.test.support.OrderService orderService;

        @Autowired
        public void setOrderService(com.bootcloud.core.test.support.OrderService orderService) {
            this.orderService = orderService;
        }

        public String getUserAndOrder() {
            return userRepository.findById("1") + " / " + orderService.findOrder("2");
        }
    }

    // --- 接口类型 + byName 回退 ---

    public interface Notifier {
        String notify(String msg);
    }

    @Component("emailNotifier")
    public static class EmailNotifier implements Notifier {
        @Override
        public String notify(String msg) {
            return "Email: " + msg;
        }
    }

    @Component("smsNotifier")
    public static class SmsNotifier implements Notifier {
        @Override
        public String notify(String msg) {
            return "SMS: " + msg;
        }
    }

    @Service
    public static class NotifierService {
        private Notifier smsNotifier;

        @Autowired
        public void setSmsNotifier(Notifier smsNotifier) {
            this.smsNotifier = smsNotifier;
        }

        public String send(String msg) {
            return smsNotifier.notify(msg);
        }
    }

    @Test
    public void testSingleTypeSetterInjection() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        SingleSetterBean bean = context.getBean(SingleSetterBean.class);
        assertNotNull(bean);
        assertEquals("User-42", bean.find("42"));

        logger.info("Single type setter injection test passed");
    }

    @Test
    public void testMixedInjection() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        MixedInjectionBean bean = context.getBean(MixedInjectionBean.class);
        assertNotNull(bean);
        assertEquals("User-1 / Order-2", bean.getUserAndOrder());

        logger.info("Mixed injection test passed");
    }

    @Test
    public void testInterfaceTypeWithNameFallback() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        NotifierService notifierService = context.getBean(NotifierService.class);
        assertNotNull(notifierService);
        assertEquals("SMS: alert", notifierService.send("alert"));

        logger.info("Interface type with byName fallback test passed");
    }
}
