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
 * 依赖注入测试
 *
 * 【测试目标】
 * 1. 字段注入（@Autowired on field）
 * 2. 构造器注入（@Autowired on constructor）
 * 3. Setter 注入（@Autowired on setter）
 * 4. byType → byName 回退
 */
public class DependencyInjectionTest {
    private static final Logger logger = LoggerFactory.getLogger(DependencyInjectionTest.class);

    // --- 字段注入 ---

    @Service
    public static class FieldInjectionBean {
        @Autowired
        private UserRepository userRepository;

        public String findUser(String id) {
            return userRepository.findById(id);
        }
    }

    // --- 构造器注入 ---

    @Service
    public static class ConstructorInjectionBean {
        private final UserRepository userRepository;

        @Autowired
        public ConstructorInjectionBean(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        public String findUser(String id) {
            return userRepository.findById(id);
        }
    }

    // --- Setter 注入 ---

    @Service
    public static class SetterInjectionBean {
        private UserRepository userRepository;

        @Autowired
        public void setUserRepository(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        public String findUser(String id) {
            return userRepository.findById(id);
        }
    }

    // --- 接口 + 多实现 + byName 回退 ---

    public interface MessageSender {
        String send(String msg);
    }

    @Component("emailSender")
    public static class EmailSender implements MessageSender {
        @Override
        public String send(String msg) {
            return "Email: " + msg;
        }
    }

    @Component("smsSender")
    public static class SmsSender implements MessageSender {
        @Override
        public String send(String msg) {
            return "SMS: " + msg;
        }
    }

    @Service
    public static class ByNameFallbackBean {
        private MessageSender smsSender;

        @Autowired
        public void setSmsSender(MessageSender smsSender) {
            this.smsSender = smsSender;
        }

        public String send(String msg) {
            return smsSender.send(msg);
        }
    }

    @Test
    public void testFieldInjection() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        FieldInjectionBean bean = context.getBean(FieldInjectionBean.class);
        assertNotNull(bean);
        assertEquals("User-123", bean.findUser("123"));

        logger.info("Field injection test passed");
    }

    @Test
    public void testConstructorInjection() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        ConstructorInjectionBean bean = context.getBean(ConstructorInjectionBean.class);
        assertNotNull(bean);
        assertEquals("User-456", bean.findUser("456"));

        logger.info("Constructor injection test passed");
    }

    @Test
    public void testSetterInjection() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        SetterInjectionBean bean = context.getBean(SetterInjectionBean.class);
        assertNotNull(bean);
        assertEquals("User-789", bean.findUser("789"));

        logger.info("Setter injection test passed");
    }

    @Test
    public void testByNameFallback() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        // MessageSender 有两个实现，byName 回退到 "smsSender"
        ByNameFallbackBean bean = context.getBean(ByNameFallbackBean.class);
        assertNotNull(bean);
        assertEquals("SMS: hello", bean.send("hello"));

        logger.info("ByName fallback test passed");
    }

    @Test
    public void testGetBeanByConcreteType() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        EmailSender emailSender = context.getBean(EmailSender.class);
        assertNotNull(emailSender);
        assertEquals("Email: test", emailSender.send("test"));

        SmsSender smsSender = context.getBean(SmsSender.class);
        assertNotNull(smsSender);
        assertEquals("SMS: test", smsSender.send("test"));

        logger.info("Get bean by concrete type test passed");
    }
}
