package com.bootcloud.concurrent;

/**
 * Semaphore（信号量）工具类
 * 
 * 基于 AQS 实现，用于控制同时访问资源的线程数量
 * 
 * @author BootCloud
 * @date 2026-04-14
 */
public class SemaphoreUtils {

    private SemaphoreUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 基于 AQS 实现的 Semaphore
     */
    public static class Semaphore {
        private final Sync sync;

        private static class Sync extends AqsUtils.SimpleAQS {
            Sync(int permits) {
                setState(permits);
            }

            int getPermits() {
                return getState();
            }

            @Override
            protected int tryAcquireShared(int arg) {
                for (;;) {
                    int available = getState();
                    int remaining = available - arg;
                    if (remaining < 0) {
                        return -1;
                    }
                    if (compareAndSetState(available, remaining)) {
                        return remaining >= 0 ? 1 : -1;
                    }
                }
            }

            @Override
            protected boolean tryReleaseShared(int arg) {
                for (;;) {
                    int current = getState();
                    int next = current + arg;
                    if (compareAndSetState(current, next)) {
                        return true;
                    }
                }
            }
        }

        /**
         * 创建 Semaphore 实例
         * 
         * @param permits 许可数量
         */
        public Semaphore(int permits) {
            if (permits < 0) {
                throw new IllegalArgumentException("permits must be non-negative");
            }
            sync = new Sync(permits);
        }

        /**
         * 获取一个许可
         */
        public void acquire() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * 获取指定数量的许可
         */
        public void acquire(int permits) throws InterruptedException {
            sync.acquireSharedInterruptibly(permits);
        }

        /**
         * 获取许可，不响应中断
         */
        public void acquireUninterruptibly(int permits) {
            sync.acquireShared(permits);
        }

        /**
         * 尝试获取一个许可
         */
        public boolean tryAcquire() {
            return sync.tryAcquireShared(1) >= 0;
        }

        /**
         * 尝试获取许可（带超时）
         */
        public boolean tryAcquire(long timeout, java.util.concurrent.TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * 释放一个许可
         */
        public void release() {
            sync.releaseShared(1);
        }

        /**
         * 释放指定数量的许可
         */
        public void release(int permits) {
            sync.releaseShared(permits);
        }

        /**
         * 获取可用许可数量
         */
        public int availablePermits() {
            return sync.getPermits();
        }

        /**
         * 获取等待队列长度
         */
        public int getQueueLength() {
            return 0;
        }
    }
}
