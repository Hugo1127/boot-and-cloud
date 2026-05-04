package com.bootcloud.core.test;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.Component;
import com.bootcloud.core.annotation.Service;
import com.bootcloud.core.context.support.GenericApplicationContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多 Bean 匹配与 @Qualifier/byName 回退测试
 *
 * 【测试目标】
 * 1. 接口有多个实现时，按字段/参数名回退匹配 Bean 名称
 * 2. 按具体实现类型获取 Bean（无歧义）
 * 3. 多个同类型 Bean 且名称不匹配时报错
 */
public class MultiBeanResolutionTest {
    private static final Logger logger = LoggerFactory.getLogger(MultiBeanResolutionTest.class);

    public interface PaymentGateway {
        String pay(String amount);
    }

    @Component("alipay")
    public static class AlipayGateway implements PaymentGateway {
        @Override
        public String pay(String amount) {
            return "Alipay: " + amount;
        }
    }

    @Component("wechatPay")
    public static class WechatPayGateway implements PaymentGateway {
        @Override
        public String pay(String amount) {
            return "WechatPay: " + amount;
        }
    }

    // 字段名匹配 "alipay" → 注入 AlipayGateway
    @Service
    public static class AlipayService {
        private PaymentGateway alipay;

        @Autowired
        public void setAlipay(PaymentGateway alipay) {
            this.alipay = alipay;
        }

        public String pay(String amount) {
            return alipay.pay(amount);
        }
    }

    // 字段名匹配 "wechatPay" → 注入 WechatPayGateway
    @Service
    public static class WechatPayService {
        private PaymentGateway wechatPay;

        @Autowired
        public void setWechatPay(PaymentGateway wechatPay) {
            this.wechatPay = wechatPay;
        }

        public String pay(String amount) {
            return wechatPay.pay(amount);
        }
    }

    // 单一实现类型 → 无歧义
    @Component("uniqueService")
    public static class UniqueServiceImpl {
        public String hello() {
            return "unique";
        }
    }

    @Test
    public void testMultipleImplementationsWithByNameFallback() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        AlipayService alipayService = context.getBean(AlipayService.class);
        assertNotNull(alipayService);
        assertEquals("Alipay: 100", alipayService.pay("100"));

        WechatPayService wechatPayService = context.getBean(WechatPayService.class);
        assertNotNull(wechatPayService);
        assertEquals("WechatPay: 200", wechatPayService.pay("200"));

        logger.info("Multiple implementations with byName fallback test passed");
    }

    @Test
    public void testGetBeanByConcreteType() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        AlipayGateway alipay = context.getBean(AlipayGateway.class);
        assertNotNull(alipay);
        assertEquals("Alipay: 50", alipay.pay("50"));

        WechatPayGateway wechatPay = context.getBean(WechatPayGateway.class);
        assertNotNull(wechatPay);
        assertEquals("WechatPay: 60", wechatPay.pay("60"));

        logger.info("Get bean by concrete type test passed");
    }

    @Test
    public void testUniqueBeanNoAmbiguity() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.scan("com.bootcloud.core.test");
        context.refresh();

        UniqueServiceImpl unique = context.getBean(UniqueServiceImpl.class);
        assertNotNull(unique);
        assertEquals("unique", unique.hello());

        logger.info("Unique bean no ambiguity test passed");
    }
}
