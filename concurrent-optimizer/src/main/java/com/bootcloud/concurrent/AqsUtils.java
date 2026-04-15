package com.bootcloud.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * AQS（AbstractQueuedSynchronizer）工具类 - 修复完整版
 * 提供 AQS 核心框架、独占锁、CountDownLatch 等实现
 * @author BootCloud
 */
public class AqsUtils {

    private AqsUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * CLH 队列节点
     */
    public static class Node {
        public static final Node SHARED = new Node();
        public static final Node EXCLUSIVE = null;

        public static final int CANCELLED =  1;
        public static final int SIGNAL    = -1;
        public static final int CONDITION = -2;
        public static final int PROPAGATE = -3;

        public volatile int waitStatus;
        public volatile Node prev;
        public volatile Node next;
        public volatile Thread thread;
        public Node nextWaiter;
        public boolean isShared;

        public Node() {
        }

        // 修复：正确构造共享/独占节点
        public Node(Thread thread, Node nextWaiter) {
            this.thread = thread;
            this.nextWaiter = nextWaiter;
            this.isShared = (nextWaiter == SHARED);
        }

        public Node predecessor() {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            return p;
        }

        public boolean isCancelled() {
            return waitStatus == CANCELLED;
        }
    }

    /**
     * 简易版 AQS 框架 - 修复线程安全与逻辑
     */
    public abstract static class SimpleAQS {
        protected volatile int state;
        protected transient volatile Node head;
        protected transient volatile Node tail;

        protected final int getState() {
            return state;
        }

        protected final void setState(int newState) {
            state = newState;
        }

        // 模拟CAS（教学用）
        protected final boolean compareAndSetState(int expect, int update) {
            synchronized (this) {
                if (state == expect) {
                    state = update;
                    return true;
                }
                return false;
            }
        }

        // 模拟节点状态CAS
        protected final boolean compareAndSetWaitStatus(Node node, int expect, int update) {
            synchronized (this) {
                if (node.waitStatus == expect) {
                    node.waitStatus = update;
                    return true;
                }
                return false;
            }
        }

        // 模拟尾节点CAS
        private boolean compareAndSetTail(Node expect, Node update) {
            synchronized (this) {
                if (tail == expect) {
                    tail = update;
                    return true;
                }
                return false;
            }
        }

        protected boolean tryAcquire(int arg) {
            throw new UnsupportedOperationException();
        }

        protected boolean tryRelease(int arg) {
            throw new UnsupportedOperationException();
        }

        protected int tryAcquireShared(int arg) {
            throw new UnsupportedOperationException();
        }

        protected boolean tryReleaseShared(int arg) {
            throw new UnsupportedOperationException();
        }

        // 独占式获取锁
        public final void acquire(int arg) {
            if (!tryAcquire(arg))
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg);
        }

        public final void acquireInterruptibly(int arg) throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            if (!tryAcquire(arg))
                doAcquireInterruptibly(arg);
        }

        public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
        }

        // 共享式获取锁
        public final void acquireShared(int arg) {
            if (tryAcquireShared(arg) < 0)
                doAcquireShared(arg);
        }

        public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            if (tryAcquireShared(arg) < 0)
                doAcquireSharedInterruptibly(arg);
        }

        public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
        }

        // 释放锁
        public final boolean release(int arg) {
            if (tryRelease(arg)) {
                Node h = head;
                if (h != null && h.waitStatus != 0)
                    unparkSuccessor(h);
                return true;
            }
            return false;
        }

        public final boolean releaseShared(int arg) {
            if (tryReleaseShared(arg)) {
                doReleaseShared();
                return true;
            }
            return false;
        }

        // 添加等待队列节点
        private Node addWaiter(Node mode) {
            Node node = new Node(Thread.currentThread(), mode);
            Node pred = tail;
            if (pred != null) {
                node.prev = pred;
                if (compareAndSetTail(pred, node)) {
                    pred.next = node;
                    return node;
                }
            }
            enq(node);
            return node;
        }

        // 自旋入队（修复）
        private Node enq(Node node) {
            synchronized (this) {
                if (tail == null) {
                    head = new Node();
                    tail = head;
                }
                node.prev = tail;
                tail.next = node;
                tail = node;
                return tail;
            }
        }

        // 独占式排队
        final boolean acquireQueued(final Node node, int arg) {
            boolean failed = true;
            try {
                boolean interrupted = false;
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null;
                        failed = false;
                        return interrupted;
                    }
                    if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
                        interrupted = true;
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        // 可中断获取
        private void doAcquireInterruptibly(int arg) throws InterruptedException {
            final Node node = addWaiter(Node.EXCLUSIVE);
            boolean failed = true;
            try {
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null;
                        failed = false;
                        return;
                    }
                    if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        // 超时获取
        private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
            if (nanosTimeout <= 0)
                return false;
            final long deadline = System.nanoTime() + nanosTimeout;
            final Node node = addWaiter(Node.EXCLUSIVE);
            boolean failed = true;
            try {
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null;
                        failed = false;
                        return true;
                    }
                    nanosTimeout = deadline - System.nanoTime();
                    if (nanosTimeout <= 0)
                        return false;
                    if (shouldParkAfterFailedAcquire(p, node))
                        LockSupport.parkNanos(this, nanosTimeout);
                    if (Thread.interrupted())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        // 共享式排队
        private void doAcquireShared(int arg) {
            final Node node = addWaiter(Node.SHARED);
            boolean failed = true;
            try {
                boolean interrupted = false;
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head) {
                        int r = tryAcquireShared(arg);
                        if (r >= 0) {
                            setHeadAndPropagate(node, r);
                            p.next = null;
                            if (interrupted)
                                selfInterrupt();
                            failed = false;
                            return;
                        }
                    }
                    if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
                        interrupted = true;
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
            final Node node = addWaiter(Node.SHARED);
            boolean failed = true;
            try {
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head) {
                        int r = tryAcquireShared(arg);
                        if (r >= 0) {
                            setHeadAndPropagate(node, r);
                            p.next = null;
                            failed = false;
                            return;
                        }
                    }
                    if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
            if (nanosTimeout <= 0)
                return false;
            final long deadline = System.nanoTime() + nanosTimeout;
            final Node node = addWaiter(Node.SHARED);
            boolean failed = true;
            try {
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head) {
                        int r = tryAcquireShared(arg);
                        if (r >= 0) {
                            setHeadAndPropagate(node, r);
                            p.next = null;
                            failed = false;
                            return true;
                        }
                    }
                    nanosTimeout = deadline - System.nanoTime();
                    if (nanosTimeout <= 0)
                        return false;
                    if (shouldParkAfterFailedAcquire(p, node))
                        LockSupport.parkNanos(this, nanosTimeout);
                    if (Thread.interrupted())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        // 共享释放 - 修复传播唤醒
        private void doReleaseShared() {
            for (;;) {
                Node h = head;
                if (h != null && h != tail) {
                    int ws = h.waitStatus;
                    if (ws == Node.SIGNAL) {
                        if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                            continue;
                        unparkSuccessor(h);
                    } else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                        continue;
                }
                if (h == head)
                    break;
            }
        }

        // 设置头节点
        private void setHead(Node node) {
            head = node;
            node.thread = null;
            node.prev = null;
        }

        // 共享传播
        private void setHeadAndPropagate(Node node, int propagate) {
            Node h = head;
            setHead(node);
            if (propagate > 0 || h == null || h.waitStatus < 0 ||
                    (h = head) == null || h.waitStatus < 0) {
                Node s = node.next;
                if (s == null || s.isShared)
                    doReleaseShared();
            }
        }

        // 取消排队
        private void cancelAcquire(Node node) {
            if (node == null)
                return;
            node.thread = null;
            Node pred = node.prev;
            while (pred.isCancelled())
                node.prev = pred = pred.prev;

            node.waitStatus = Node.CANCELLED;
            synchronized (this) {
                if (node == tail)
                    compareAndSetTail(node, pred);
                else {
                    Node next = node.next;
                    if (!pred.isCancelled()) {
                        pred.next = next;
                        if (next != null)
                            next.prev = pred;
                    }
                }
            }
        }

        // 核心：判断是否可以挂起线程（修复CAS更新状态）
        private boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
            int ws = pred.waitStatus;
            if (ws == Node.SIGNAL)
                return true;
            if (ws > 0) {
                do {
                    node.prev = pred = pred.prev;
                } while (pred.isCancelled());
                pred.next = node;
            } else {
                compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
            }
            return false;
        }

        // 挂起线程并检查中断
        private final boolean parkAndCheckInterrupt() {
            LockSupport.park(this);
            return Thread.interrupted();
        }

        // 唤醒后继节点
        private void unparkSuccessor(Node node) {
            int ws = node.waitStatus;
            if (ws < 0)
                compareAndSetWaitStatus(node, ws, 0);

            Node s = node.next;
            if (s != null && !s.isCancelled())
                LockSupport.unpark(s.thread);
        }

        // 自我中断
        private void selfInterrupt() {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 基于 AQS 实现的可重入锁（修复）
     */
    public static class ReentrantLock implements Lock {
        private final Sync sync = new Sync();

        private static class Sync extends SimpleAQS {
            private transient Thread exclusiveOwnerThread;

            @Override
            protected boolean tryAcquire(int arg) {
                Thread current = Thread.currentThread();
                int c = getState();
                if (c == 0) {
                    if (compareAndSetState(0, arg)) {
                        exclusiveOwnerThread = current;
                        return true;
                    }
                } else if (current == exclusiveOwnerThread) {
                    int next = c + arg;
                    setState(next);
                    return true;
                }
                return false;
            }

            @Override
            protected boolean tryRelease(int arg) {
                if (Thread.currentThread() != exclusiveOwnerThread)
                    throw new IllegalMonitorStateException();
                int next = getState() - arg;
                boolean free = false;
                if (next == 0) {
                    free = true;
                    exclusiveOwnerThread = null;
                }
                setState(next);
                return free;
            }

            // 修复：tryLock支持可重入
            protected boolean tryAcquireNonReentrant(int arg) {
                return tryAcquire(arg);
            }
        }

        @Override
        public void lock() {
            sync.acquire(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquireNonReentrant(1);
        }

        @Override
        public boolean tryLock(long time, java.util.concurrent.TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(time));
        }

        @Override
        public void unlock() {
            sync.release(1);
        }

        @Override
        public java.util.concurrent.locks.Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public boolean isLocked() {
            return sync.getState() != 0;
        }

        public int getHoldCount() {
            return sync.getState();
        }
    }

    /**
     * 基于 AQS 实现的 CountDownLatch（修复）
     */
    public static class CountDownLatch {
        private final Sync sync;

        private static class Sync extends SimpleAQS {
            Sync(int count) {
                setState(count);
            }

            int getCount() {
                return getState();
            }

            @Override
            protected int tryAcquireShared(int acquires) {
                return (getState() == 0) ? 1 : -1;
            }

            @Override
            protected boolean tryReleaseShared(int releases) {
                for (;;) {
                    int c = getState();
                    if (c == 0)
                        return false;
                    int next = c - 1;
                    if (compareAndSetState(c, next))
                        return next == 0;
                }
            }
        }

        public CountDownLatch(int count) {
            if (count < 0)
                throw new IllegalArgumentException("count < 0");
            sync = new Sync(count);
        }

        public void await() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        public boolean await(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        public void countDown() {
            sync.releaseShared(1);
        }

        public long getCount() {
            return sync.getCount();
        }
    }
}