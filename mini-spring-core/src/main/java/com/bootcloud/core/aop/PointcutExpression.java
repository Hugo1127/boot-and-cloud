package com.bootcloud.core.aop;

import java.lang.reflect.Method;

public class PointcutExpression {
    private final String expression;

    public PointcutExpression(String expression) {
        this.expression = expression;
    }

    public boolean matches(Class<?> targetClass, Method targetMethod) {
        if (expression == null || expression.isEmpty()) {
            return false;
        }

        // 格式: "完整类名.方法模式"
        // 方法模式可能为: methodName, methodName(), methodName(..), *(..), *
        // 策略: 找到第一个 "(" 或最后的 "."（取较前者）来分割
        int parenIndex = expression.indexOf('(');
        int lastDot = expression.lastIndexOf('.');

        int splitIndex;
        if (parenIndex != -1 && parenIndex < lastDot) {
            // "(" 出现在最后一个 "." 之前，说明 "." 是方法模式的一部分（如 ".."）
            // 应该取 "(" 之前的最后一个 "."
            splitIndex = expression.lastIndexOf('.', parenIndex - 1);
        } else {
            splitIndex = lastDot;
        }

        if (splitIndex == -1) {
            return false;
        }

        String classPattern = expression.substring(0, splitIndex);
        String methodPattern = expression.substring(splitIndex + 1);

        return matchesClassPattern(classPattern, targetClass) && matchesMethodPattern(methodPattern, targetMethod);
    }

    private boolean matchesClassPattern(String pattern, Class<?> targetClass) {
        String className = targetClass.getName();

        if ("*".equals(pattern)) {
            return true;
        }

        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return className.startsWith(prefix);
        }

        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return className.startsWith(prefix);
        }

        if (pattern.startsWith("*")) {
            String suffix = pattern.substring(1);
            return className.endsWith(suffix);
        }

        if (pattern.contains("*")) {
            String regex = java.util.regex.Pattern.quote(pattern).replace("\\*", ".*");
            return className.matches(regex);
        }

        return pattern.equals(className);
    }

    private boolean matchesMethodPattern(String pattern, Method targetMethod) {
        String methodName = targetMethod.getName();

        if ("*".equals(pattern)) {
            return true;
        }

        if (pattern.equals("*(..)")) {
            return true;
        }

        // "methodName(..)" 匹配指定名称的任意参数方法
        if (pattern.endsWith("(..)")) {
            String baseName = pattern.substring(0, pattern.length() - 4);
            return baseName.equals(methodName);
        }

        if (pattern.endsWith("()")) {
            String baseName = pattern.substring(0, pattern.length() - 2);
            return baseName.equals(methodName);
        }

        return pattern.equals(methodName);
    }
}
