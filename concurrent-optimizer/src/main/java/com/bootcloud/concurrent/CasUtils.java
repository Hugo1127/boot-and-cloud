package com.bootcloud.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * CAS（Compare And Swap）工具类
 * 
 * 提供 CAS 核心功能、ABA 问题演示与解决方案
 * 
 * @author BootCloud
 * @date 2026-04-14
 */
public class CasUtils {

    private CasUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 基于 AtomicReference 的无锁栈（用于演示 ABA 问题）
     */
    public static class LockFreeStack {
        private final AtomicReference<Node> top = new AtomicReference<>();

        /**
         * 栈节点
         */
        public static class Node {
            int value;
            Node next;

            public Node(int value, Node next) {
                this.value = value;
                this.next = next;
            }
        }

        /**
         * 压栈操作
         */
        public void push(int value) {
            Node oldTop;
            Node newTop;
            do {
                oldTop = top.get();
                newTop = new Node(value, oldTop);
            } while (!top.compareAndSet(oldTop, newTop));
        }

        /**
         * 弹栈操作
         */
        public Integer pop() {
            Node oldTop;
            Node newTop;
            do {
                oldTop = top.get();
                if (oldTop == null) {
                    return null;
                }
                newTop = oldTop.next;
            } while (!top.compareAndSet(oldTop, newTop));
            return oldTop.value;
        }

        /**
         * 获取栈顶节点
         */
        public Node getTop() {
            return top.get();
        }
    }

    /**
     * 基于 AtomicStampedReference 的无锁栈（解决 ABA 问题）
     */
    public static class StampedLockFreeStack {
        private final AtomicStampedReference<Node> top = new AtomicStampedReference<>(null, 0);

        /**
         * 栈节点
         */
        public static class Node {
            int value;
            Node next;

            public Node(int value, Node next) {
                this.value = value;
                this.next = next;
            }
        }

        /**
         * 压栈操作（带版本号）
         */
        public void push(int value) {
            int[] stampHolder = new int[1];
            Node oldTop;
            Node newTop;
            do {
                oldTop = top.getReference();
                int oldStamp = top.getStamp();
                newTop = new Node(value, oldTop);
                int newStamp = oldStamp + 1;
                if (top.compareAndSet(oldTop, newTop, oldStamp, newStamp)) {
                    break;
                }
            } while (true);
        }

        /**
         * 弹栈操作（带版本号）
         */
        public Integer pop() {
            int[] stampHolder = new int[1];
            Node oldTop;
            Node newTop;
            do {
                oldTop = top.getReference();
                int oldStamp = top.getStamp();
                if (oldTop == null) {
                    return null;
                }
                newTop = oldTop.next;
                int newStamp = oldStamp + 1;
                if (top.compareAndSet(oldTop, newTop, oldStamp, newStamp)) {
                    break;
                }
            } while (true);
            return oldTop.value;
        }

        /**
         * 获取当前版本号
         */
        public int getStamp() {
            return top.getStamp();
        }

        /**
         * 获取栈顶引用
         */
        public Node getReference() {
            return top.getReference();
        }
    }

    /**
     * 基于 AtomicInteger 的 CAS 自旋锁
     */
    public static class CasSpinLock {
        private final AtomicInteger state = new AtomicInteger(0);

        /**
         * 获取锁
         */
        public void lock() {
            while (!state.compareAndSet(0, 1)) {
                Thread.yield();
            }
        }

        /**
         * 释放锁
         */
        public void unlock() {
            state.set(0);
        }

        /**
         * 尝试获取锁
         */
        public boolean tryLock() {
            return state.compareAndSet(0, 1);
        }

        /**
         * 检查是否被持有
         */
        public boolean isLocked() {
            return state.get() == 1;
        }
    }
}
