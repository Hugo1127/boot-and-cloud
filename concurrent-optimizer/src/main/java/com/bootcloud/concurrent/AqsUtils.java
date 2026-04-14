package com.bootcloud.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * AQS（AbstractQueuedSynchronizer）工具类
 * 
 * 提供 AQS 核心框架、独占锁、CountDownLatch 等实现
 * 
 * @author BootCloud
 * @date 2026-04-14
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
        public static final int EXCLUSIVE = 1;

        public static final int CANCELLED = 1;
        public static final int SIGNAL = -1;
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

        public Node(Thread thread, int mode) {
            this.thread = thread;
            this.isShared = (mode == SHARED.hashCode());
        }

        public Node predecessor() {
            Node p = prev;
            if (p == null) {
                throw new NullPointerException();
            }
            return p;
        }

        public boolean isCancelled() {
            return waitStatus == CANCELLED;
        }
    }

    /**
     * 简易版 AQS 框架
     */
    public abstract static class SimpleAQS {
        protected volatile int state;
        protected transient Node head;
        protected transient Node tail;

        protected final int getState() {
            return state;
        }

        protected final void setState(int newState) {
            state = newState;
        }

        protected final boolean compareAndSetState(int expect, int update) {
            synchronized (this) {
                if (state == expect) {
                    state = update;
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

        public final void acquire(int arg) {
            if (!tryAcquire(arg)) {
                Node node = addWaiter(Node.EXCLUSIVE);
                acquireQueued(node, arg);
            }
        }

        public final void acquireInterruptibly(int arg) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (!tryAcquire(arg)) {
                doAcquireInterruptibly(arg);
            }
        }

        public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (tryAcquire(arg)) {
                return true;
            }
            if (nanosTimeout <= 0) {
                return false;
            }
            doAcquireInterruptibly(arg);
            return true;
        }

        public final void acquireShared(int arg) {
            if (tryAcquireShared(arg) < 0) {
                doAcquireShared(arg);
            }
        }

        public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (tryAcquireShared(arg) < 0) {
                doAcquireShared(arg);
            }
        }

        public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (tryAcquireShared(arg) >= 0) {
                return true;
            }
            if (nanosTimeout <= 0) {
                return false;
            }
            doAcquireShared(arg);
            return true;
        }

        public final boolean release(int arg) {
            if (tryRelease(arg)) {
                Node h = head;
                if (h != null && h.waitStatus != 0) {
                    unparkSuccessor(h);
                }
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

        private Node addWaiter(int mode) {
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

        private Node enq(Node node) {
            for (;;) {
                Node t = tail;
                if (t == null) {
                    if (compareAndSetHead(new Node())) {
                        tail = head;
                    }
                } else {
                    node.prev = t;
                    if (compareAndSetTail(t, node)) {
                        t.next = node;
                        return t;
                    }
                }
            }
        }

        private void acquireQueued(Node node, int arg) {
            boolean failed = true;
            try {
                boolean interrupted = false;
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null;
                        failed = false;
                        return;
                    }
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            parkAndCheckInterrupt()) {
                        interrupted = true;
                    }
                }
            } finally {
                if (failed) {
                    cancelAcquire(node);
                }
            }
        }

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
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            parkAndCheckInterrupt()) {
                        throw new InterruptedException();
                    }
                }
            } finally {
                if (failed) {
                    cancelAcquire(node);
                }
            }
        }

        private void doAcquireShared(int arg) {
            final Node node = addWaiter(Node.SHARED.hashCode());
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
                            if (interrupted) {
                                selfInterrupt();
                            }
                            failed = false;
                            return;
                        }
                    }
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            parkAndCheckInterrupt()) {
                        interrupted = true;
                    }
                }
            } finally {
                if (failed) {
                    cancelAcquire(node);
                }
            }
        }

        private void doReleaseShared() {
            for (;;) {
                Node h = head;
                if (h != null && h != tail) {
                    int ws = h.waitStatus;
                    if (ws == Node.SIGNAL) {
                        if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) {
                            continue;
                        }
                        unparkSuccessor(h);
                    } else if (ws == 0 &&
                            !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)) {
                        continue;
                    }
                    if (h == tail) {
                        continue;
                    }
                }
                if (h == head) {
                    break;
                }
            }
        }

        private void setHead(Node node) {
            head = node;
            node.thread = null;
            node.prev = null;
        }

        private void setHeadAndPropagate(Node node, int propagate) {
            Node h = head;
            setHead(node);

            if (propagate > 0 && h != null && h.waitStatus < 0) {
                Node s = h.next;
                if (s == null || s.isShared) {
                    doReleaseShared();
                }
            }
        }

        private void cancelAcquire(Node node) {
            if (node == null)
                return;
            node.thread = null;

            Node pred = node.prev;
            while (pred.waitStatus > 0) {
                node.prev = pred.prev;
                pred = pred.prev;
            }

            Node predNext = pred.next;
            node.waitStatus = Node.CANCELLED;

            if (node == tail && compareAndSetTail(node, pred)) {
                compareAndSetNext(pred, predNext, null);
            } else {
                int ws = pred.waitStatus;
                if (ws == Node.SIGNAL ||
                        (ws == 0 && !compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) {
                    Thread thread = pred.thread;
                    if (thread != null) {
                        LockSupport.unpark(thread);
                    }
                }
            }
        }

        private boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
            int ws = pred.waitStatus;
            if (ws == Node.SIGNAL) {
                return true;
            }
            if (ws > 0) {
                do {
                    node.prev = pred.prev;
                    pred = pred.prev;
                } while (pred.waitStatus > 0);
                pred.next = node;
            } else {
                compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
            }
            return false;
        }

        private final boolean parkAndCheckInterrupt() {
            LockSupport.park(this);
            return Thread.interrupted();
        }

        private void unparkSuccessor(Node node) {
            int ws = node.waitStatus;
            if (ws < 0) {
                compareAndSetWaitStatus(node, ws, 0);
            }

            Node s = node.next;
            if (s == null || s.waitStatus > 0) {
                s = null;
                for (Node t = tail; t != null && t != node; t = t.prev) {
                    if (t.waitStatus <= 0) {
                        s = t;
                    }
                }
            }

            if (s != null) {
                LockSupport.unpark(s.thread);
            }
        }

        private boolean compareAndSetHead(Node update) {
            synchronized (this) {
                if (head == null) {
                    head = update;
                    return true;
                }
                return false;
            }
        }

        private boolean compareAndSetTail(Node expect, Node update) {
            synchronized (this) {
                if (tail == expect) {
                    tail = update;
                    return true;
                }
                return false;
            }
        }

        private boolean compareAndSetNext(Node node, Node expect, Node update) {
            synchronized (node) {
                if (node.next == expect) {
                    node.next = update;
                    return true;
                }
                return false;
            }
        }

        private boolean compareAndSetWaitStatus(Node node, int expect, int update) {
            synchronized (node) {
                if (node.waitStatus == expect) {
                    node.waitStatus = update;
                    return true;
                }
                return false;
            }
        }

        private void selfInterrupt() {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 基于 AQS 实现的可重入锁
     */
    public static class ReentrantLock implements Lock {
        private final Sync sync = new Sync();

        private static class Sync extends SimpleAQS {
            private transient Thread exclusiveOwnerThread;

            @Override
            protected boolean tryAcquire(int arg) {
                Thread current = Thread.currentThread();
                if (state == 0) {
                    if (compareAndSetState(0, arg)) {
                        exclusiveOwnerThread = current;
                        return true;
                    }
                } else if (current == exclusiveOwnerThread) {
                    int nextCount = getState() + arg;
                    setState(nextCount);
                    return true;
                }
                return false;
            }

            @Override
            protected boolean tryRelease(int arg) {
                Thread current = Thread.currentThread();
                if (current == exclusiveOwnerThread) {
                    int nextCount = getState() - arg;
                    if (nextCount == 0) {
                        exclusiveOwnerThread = null;
                    }
                    setState(nextCount);
                    return true;
                }
                return false;
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
            return sync.tryAcquire(1);
        }

        @Override
        public boolean tryLock(long time, java.util.concurrent.TimeUnit unit)
                throws InterruptedException {
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
            return sync.getState() > 0;
        }

        public int getHoldCount() {
            return sync.getState();
        }
    }

    /**
     * 基于 AQS 实现的 CountDownLatch
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
            protected int tryAcquireShared(int arg) {
                return getState() == 0 ? 1 : -1;
            }

            @Override
            protected boolean tryReleaseShared(int arg) {
                for (;;) {
                    int c = getState();
                    if (c == 0) {
                        return false;
                    }
                    int nextCount = c - 1;
                    if (compareAndSetState(c, nextCount)) {
                        return nextCount == 0;
                    }
                }
            }
        }

        public CountDownLatch(int count) {
            if (count < 0) {
                throw new IllegalArgumentException("count < 0");
            }
            sync = new Sync(count);
        }

        public void await() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        public boolean await(long timeout, java.util.concurrent.TimeUnit unit)
                throws InterruptedException {
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
