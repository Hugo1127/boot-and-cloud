package com.bootcloud.core.test;

import com.bootcloud.core.aop.PointcutExpression;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PointcutExpression 表达式匹配测试
 *
 * PointcutExpression 将表达式按 "." 分割，取第一段作为类匹配模式，
 * 取第二段作为方法匹配模式（其余段被忽略）。
 * 例如: "com.bootcloud.core.test.TestService.findUser" →
 *   类模式: "com"，方法模式: "bootcloud"
 *
 * 因此实际使用中表达式格式为: "完整类名.方法名"
 */
public class PointcutExpressionTest {

    @Test
    public void testExactClassAndMethodMatch() {
        // 格式: "完整类名.方法名"
        String expression = PointcutExpressionTest.class.getName() + ".testExactClassAndMethodMatch";
        PointcutExpression pointcut = new PointcutExpression(expression);

        assertTrue(pointcut.matches(
                PointcutExpressionTest.class,
                getDeclaredMethod("testExactClassAndMethodMatch")));
    }

    @Test
    public void testWildcardMethodMatch() {
        // 类名精确 + *(..) 匹配任意方法
        String expression = PointcutExpressionTest.class.getName() + ".*(..)";
        PointcutExpression pointcut = new PointcutExpression(expression);

        assertTrue(pointcut.matches(
                PointcutExpressionTest.class,
                getDeclaredMethod("testWildcardMethodMatch")));
    }

    @Test
    public void testNoMatch() {
        PointcutExpression expression = new PointcutExpression(
                "com.bootcloud.other.SomeClass.someMethod");

        assertFalse(expression.matches(
                PointcutExpressionTest.class,
                getDeclaredMethod("testNoMatch")));
    }

    @Test
    public void testEmptyExpression() {
        PointcutExpression expression = new PointcutExpression("");
        assertFalse(expression.matches(PointcutExpressionTest.class, getDeclaredMethod("testEmptyExpression")));
    }

    @Test
    public void testNullExpression() {
        PointcutExpression expression = new PointcutExpression(null);
        assertFalse(expression.matches(PointcutExpressionTest.class, getDeclaredMethod("testNullExpression")));
    }

    @Test
    public void testMethodWithNoArgs() {
        String expression = PointcutExpressionTest.class.getName() + ".noArgsMethod()";
        PointcutExpression pointcut = new PointcutExpression(expression);

        assertTrue(pointcut.matches(
                PointcutExpressionTest.class,
                getDeclaredMethod("noArgsMethod")));
    }

    @Test
    public void testMethodWithArgsPattern() {
        String expression = PointcutExpressionTest.class.getName() + ".methodWithArgs(..)";
        PointcutExpression pointcut = new PointcutExpression(expression);

        assertTrue(pointcut.matches(
                PointcutExpressionTest.class,
                getDeclaredMethod("methodWithArgs", String.class, int.class)));
    }

    // --- Helper methods for testing ---

    private Method getDeclaredMethod(String name, Class<?>... paramTypes) {
        try {
            return PointcutExpressionTest.class.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public void noArgsMethod() {
    }

    public void methodWithArgs(String arg1, int arg2) {
    }
}
