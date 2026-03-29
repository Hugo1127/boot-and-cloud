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

        String[] parts = expression.split("\\.");
        if (parts.length < 2) {
            return false;
        }

        String classPattern = parts[0];
        String methodPattern = parts[1];

        return matchesClassPattern(classPattern, targetClass) && matchesMethodPattern(methodPattern, targetMethod);
    }

    private boolean matchesClassPattern(String pattern, Class<?> targetClass) {
        if ("*".equals(pattern)) {
            return true;
        }

        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return targetClass.getName().startsWith(prefix);
        }

        if (pattern.startsWith("*")) {
            String suffix = pattern.substring(1);
            return targetClass.getName().endsWith(suffix);
        }

        if (pattern.contains("*")) {
            String[] parts = pattern.split("\\*");
            String className = targetClass.getName();
            boolean matches = true;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].isEmpty()) {
                    continue;
                }
                int index = className.indexOf(parts[i]);
                if (index == -1) {
                    matches = false;
                    break;
                }
                className = className.substring(index + parts[i].length());
            }
            return matches;
        }

        return pattern.equals(targetClass.getName());
    }

    private boolean matchesMethodPattern(String pattern, Method targetMethod) {
        if ("*".equals(pattern)) {
            return true;
        }

        if (pattern.endsWith("()")) {
            String methodName = pattern.substring(0, pattern.length() - 2);
            return methodName.equals(targetMethod.getName());
        }

        if (pattern.endsWith("*(..)")) {
            String methodName = pattern.substring(0, pattern.length() - 5);
            return methodName.equals(targetMethod.getName());
        }

        return pattern.equals(targetMethod.getName());
    }
}
