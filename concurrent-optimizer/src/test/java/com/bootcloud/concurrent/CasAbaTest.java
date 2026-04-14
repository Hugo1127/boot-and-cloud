package com.bootcloud.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CasUtils 单元测试
 * 
 * 测试 CAS、ABA 问题及解决方案
 * 
 * @author BootCloud
 * @date 2026-04-14
 */
public class CasAbaTest {

    @Test
    @DisplayName("测试无锁栈的基本功能")
    public void testLockFreeStackBasic() {
        CasUtils.LockFreeStack stack = new CasUtils.LockFreeStack();
        
        stack.push(1);
        stack.push(2);
        stack.push(3);
        
        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
        assertNull(stack.pop());
    }

    @Test
    @DisplayName("测试 ABA 问题复现")
    public void testAbaProblem() throws InterruptedException {
        CasUtils.LockFreeStack stack = new CasUtils.LockFreeStack();
        
        stack.push(3);
        stack.push(2);
        stack.push(1);
        
        CasUtils.LockFreeStack.Node initialTop = stack.getTop();
        
        Thread thread2 = new Thread(() -> {
            stack.pop();
            stack.pop();
            stack.push(4);
        });
        
        thread2.start();
        thread2.join();
        
        CasUtils.LockFreeStack.Node currentTop = stack.getTop();
        assertNotSame(initialTop, currentTop, "栈顶引用应该已经改变");
        assertEquals(4, currentTop.value, "栈顶值应该是 4");
    }

    @Test
    @DisplayName("测试 AtomicStampedReference 解决 ABA 问题")
    public void testStampedReferenceSolution() throws InterruptedException {
        CasUtils.StampedLockFreeStack stack = new CasUtils.StampedLockFreeStack();
        
        stack.push(3);
        stack.push(2);
        stack.push(1);
        
        int initialStamp = stack.getStamp();
        CasUtils.StampedLockFreeStack.Node initialRef = stack.getReference();
        
        Thread thread2 = new Thread(() -> {
            stack.pop();
            stack.pop();
            stack.push(4);
        });
        
        thread2.start();
        thread2.join();
        
        int currentStamp = stack.getStamp();
        assertNotEquals(initialStamp, currentStamp, "版本号应该已经改变");
    }

    @Test
    @DisplayName("测试带版本号的栈功能")
    public void testStampedLockFreeStackBasic() {
        CasUtils.StampedLockFreeStack stack = new CasUtils.StampedLockFreeStack();
        
        stack.push(1);
        stack.push(2);
        stack.push(3);
        
        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
        assertNull(stack.pop());
    }

    @Test
    @DisplayName("测试 CAS 自旋锁的基本功能")
    public void testCasSpinLockBasic() {
        CasUtils.CasSpinLock lock = new CasUtils.CasSpinLock();
        
        assertFalse(lock.isLocked());
        
        lock.lock();
        assertTrue(lock.isLocked());
        
        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    @DisplayName("测试 CAS 自旋锁的 tryLock 方法")
    public void testCasSpinLockTryLock() {
        CasUtils.CasSpinLock lock = new CasUtils.CasSpinLock();
        
        assertTrue(lock.tryLock());
        assertFalse(lock.tryLock());
        
        lock.unlock();
        assertTrue(lock.tryLock());
        
        lock.unlock();
    }

    @Test
    @DisplayName("测试 CAS 自旋锁的线程安全")
    public void testCasSpinLockThreadSafety() throws InterruptedException {
        CasUtils.CasSpinLock lock = new CasUtils.CasSpinLock();
        int[] counter = new int[1];
        int threadCount = 10;
        int iterations = 1000;
        
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    lock.lock();
                    try {
                        counter[0]++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        assertEquals(threadCount * iterations, counter[0], "计数器应该正确累加");
    }

    @Test
    @DisplayName("测试多个线程使用无锁栈")
    public void testLockFreeStackMultiThreaded() throws InterruptedException {
        CasUtils.LockFreeStack stack = new CasUtils.LockFreeStack();
        int threadCount = 5;
        int pushCount = 100;
        
        Thread[] pushers = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            pushers[i] = new Thread(() -> {
                for (int j = 0; j < pushCount; j++) {
                    stack.push(threadId * pushCount + j);
                }
            });
            pushers[i].start();
        }
        
        for (Thread thread : pushers) {
            thread.join();
        }
        
        int totalPops = 0;
        while (stack.pop() != null) {
            totalPops++;
        }
        
        assertEquals(threadCount * pushCount, totalPops, "所有压入的元素都应该被弹出");
    }
}
