package com.bootcloud.core.test;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.Component;
import com.bootcloud.core.annotation.PostConstruct;
import com.bootcloud.core.annotation.Service;
import com.bootcloud.core.context.support.GenericApplicationContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 Setter 注入、byName 回退、多匹配处理
 */
public class SetterInjectionTest {
    private static final Logger logger = LoggerFactory.getLogger(SetterInjectionTest.class);

    // --- 接口与多个实现 ---

    public interface MessageService {
        String send(String msg);
    }

    @Component("emailService")
    public static class EmailService implements MessageService {
        @Override
        public String send(String msg) {
            return "Email: " + msg;
        }
    }

    @Component("smsService")
    public static class SmsService implements MessageService {
        @Override
        public String send(String msg) {
            return "SMS: " + msg;
        }
    }

    // --- Setter 注入：字段名与 Bean 名匹配（byName 回退）---

    @Service
    public static class SmsNotificationService {
        private SmsService smsService;

        @Autowired
        public void setSmsService(SmsService smsService) {
            this.smsService = smsService;
        }

        public String notify(String msg) {
            return smsService.send(msg);
        }
    }

    // --- 字段注入 + 单一类型（无歧义）---

    @Service
    public static class EmailNotifier {
        private EmailService emailService;

        @Autowired
        public void setEmailService(EmailService emailService) {
            this.emailService = emailService;
        }

        public String send(String msg) {
            return emailService.send(msg);
        }
    }

    // --- 构造器注入 Bean ---

    @Service
    public static class ConstructorBean {
        private final UserRepository userRepository;

        @Autowired
        public ConstructorBean(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        public String findUser(String id) {
            return userRepository.findById(id);
        }
    }

    // --- 接口类型 + byName 回退 ---

    @Service
    public static class PreferSmsService {
        private MessageService smsService;

        @Autowired
        public void setSmsService(MessageService smsService) {
            this.smsService = smsService;
        }

        public String notify(String msg) {
            return smsService.send(msg);
        }
    }

    @Test
    public void testSetterInjectionWithSingleType() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        // SmsNotificationService 通过 Setter 注入 SmsService（单一类型，无歧义）
        SmsNotificationService smsBean = context.getBean(SmsNotificationService.class);
        assertNotNull(smsBean);
        assertEquals("SMS: hello", smsBean.notify("hello"));

        EmailNotifier emailBean = context.getBean(EmailNotifier.class);
        assertNotNull(emailBean);
        assertEquals("Email: world", emailBean.send("world"));

        logger.info("Setter injection with single type test passed");
    }

    @Test
    public void testConstructorInjection() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        ConstructorBean constructorBean = context.getBean(ConstructorBean.class);
        assertNotNull(constructorBean);
        assertEquals("User-789", constructorBean.findUser("789"));

        logger.info("Constructor injection test passed");
    }

    @Test
    public void testMultiBeanByType() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        // 按具体类型获取应该成功
        EmailService emailService = context.getBean(EmailService.class);
        assertNotNull(emailService);
        assertEquals("Email: hello", emailService.send("hello"));

        SmsService smsService = context.getBean(SmsService.class);
        assertNotNull(smsService);
        assertEquals("SMS: world", smsService.send("world"));

        logger.info("Multi-bean by type test passed");
    }

    @Test
    public void testInterfaceTypeWithNameFallback() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        // PreferSmsService 的字段类型是 MessageService（接口），
        // 但有两个实现：emailService 和 smsService。
        // byName 回退：字段名 "smsService" 匹配 Bean 名 "smsService"
        PreferSmsService preferSms = context.getBean(PreferSmsService.class);
        assertNotNull(preferSms);
        assertEquals("SMS: test", preferSms.notify("test"));

        logger.info("Interface type with byName fallback test passed");
    }
}
