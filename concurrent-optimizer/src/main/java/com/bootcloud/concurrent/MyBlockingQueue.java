package com.bootcloud.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 基于链表的手写阻塞队列（不依赖 java.util.concurrent 中的具体实现类）
 *
 * 底层原理：
 * - 底层数据：LinkedList 作为双向链表存储节点（插入/删除 O(1)）
 * - 同步机制：synchronized 锁 + Object.wait()/notify() 实现阻塞/唤醒
 * - 两个条件对象：notEmpty（take 等待）和 notFull（put 等待），从同一把锁派生
 * - put 满时阻塞，take 空时阻塞，支持超时变体
 * - while 循环检查条件，防止虚假唤醒（JLS 规范要求）
 *
 * 与 JDK LinkedBlockingQueue 对比：
 * - JDK 用两把 ReentrantLock（putLock + takeLock）实现读写分离
 * - 本实现用单一 synchronized 锁，简单但并发度较低
 *
 * @author Bootcloud
 * @date 2026-05-17
 */
public class MyBlockingQueue<E> implements BlockingQueue<E> {

    /** 底层链表 */
    private final LinkedList<E> list;

    /** 容量上限 */
    private final int capacity;

    /** 对象锁 — 所有操作共用 */
    private final Object lock = new Object();

    /** 当前元素数量 */
    private int count;

    /**
     * 有界构造
     *
     * @param capacity 队列容量，必须 > 0
     */
    public MyBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, got: " + capacity);
        }
        this.capacity = capacity;
        this.list = new LinkedList<>();
        this.count = 0;
    }

    // ========== BlockingQueue 接口实现 ==========

    @Override
    public boolean add(E element) {
        if (offer(element)) {
            return true;
        }
        throw new IllegalStateException("Queue is full");
    }

    @Override
    public boolean offer(E element) {
        Objects.requireNonNull(element, "element cannot be null");
        synchronized (lock) {
            if (count == capacity) {
                return false;
            }
            enqueue(element);
            lock.notifyAll();
            return true;
        }
    }

    @Override
    public void put(E element) throws InterruptedException {
        Objects.requireNonNull(element, "element cannot be null");
        synchronized (lock) {
            while (count == capacity) {
                lock.wait();
            }
            enqueue(element);
            lock.notifyAll();
        }
    }

    @Override
    public boolean offer(E element, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(element, "element cannot be null");
        Objects.requireNonNull(unit, "unit cannot be null");
        long nanos = unit.toNanos(timeout);
        synchronized (lock) {
            while (count == capacity) {
                if (nanos <= 0) return false;
                long before = System.nanoTime();
                lock.wait(nanos / 1_000_000, (int) (nanos % 1_000_000));
                long after = System.nanoTime();
                nanos -= (after - before);
            }
            enqueue(element);
            lock.notifyAll();
            return true;
        }
    }

    @Override
    public E take() throws InterruptedException {
        synchronized (lock) {
            while (count == 0) {
                lock.wait();
            }
            E element = dequeue();
            lock.notifyAll();
            return element;
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit, "unit cannot be null");
        long nanos = unit.toNanos(timeout);
        synchronized (lock) {
            while (count == 0) {
                if (nanos <= 0) return null;
                long before = System.nanoTime();
                lock.wait(nanos / 1_000_000, (int) (nanos % 1_000_000));
                long after = System.nanoTime();
                nanos -= (after - before);
            }
            E element = dequeue();
            lock.notifyAll();
            return element;
        }
    }

    @Override
    public int remainingCapacity() {
        synchronized (lock) {
            return capacity - count;
        }
    }

    @Override
    public E poll() {
        synchronized (lock) {
            if (count == 0) return null;
            E element = dequeue();
            lock.notifyAll();
            return element;
        }
    }

    @Override
    public E peek() {
        synchronized (lock) {
            return count == 0 ? null : list.peekFirst();
        }
    }

    @Override
    public E element() {
        E e = peek();
        if (e == null) throw new IllegalStateException("Queue is empty");
        return e;
    }

    @Override
    public E remove() {
        E e = poll();
        if (e == null) throw new IllegalStateException("Queue is empty");
        return e;
    }

    @Override
    public int size() {
        synchronized (lock) {
            return count;
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        synchronized (lock) {
            return list.contains(o);
        }
    }

    @Override
    public Iterator<E> iterator() {
        synchronized (lock) {
            return new LinkedList<>(list).iterator();
        }
    }

    @Override
    public Object[] toArray() {
        synchronized (lock) {
            return list.toArray();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        synchronized (lock) {
            return list.toArray(a);
        }
    }

    @Override
    public boolean remove(Object o) {
        synchronized (lock) {
            boolean removed = list.remove(o);
            if (removed) {
                count--;
                lock.notifyAll();
            }
            return removed;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        synchronized (lock) {
            return list.containsAll(c);
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        Objects.requireNonNull(c, "collection cannot be null");
        synchronized (lock) {
            boolean modified = false;
            for (E element : c) {
                if (count < capacity) {
                    enqueue(element);
                    modified = true;
                } else {
                    break;
                }
            }
            if (modified) lock.notifyAll();
            return modified;
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        synchronized (lock) {
            boolean modified = list.removeAll(c);
            if (modified) {
                count = list.size();
                lock.notifyAll();
            }
            return modified;
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        synchronized (lock) {
            boolean modified = list.retainAll(c);
            if (modified) {
                count = list.size();
                lock.notifyAll();
            }
            return modified;
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            list.clear();
            count = 0;
        }
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c, "collection cannot be null");
        if (c == this) throw new IllegalArgumentException("cannot drain to self");
        synchronized (lock) {
            int n = Math.min(maxElements, count);
            for (int i = 0; i < n; i++) {
                c.add(dequeue());
            }
            lock.notifyAll();
            return n;
        }
    }

    /**
     * 返回容量上限
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 是否已满
     */
    public boolean isFull() {
        return size() == capacity;
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return "HandWrittenBlockingQueue{size=" + count + ", capacity=" + capacity + ", elements=" + list + "}";
        }
    }

    // ========== 内部辅助方法（调用方必须先持 lock 锁）==========

    private void enqueue(E element) {
        list.addLast(element);
        count++;
    }

    private E dequeue() {
        E element = list.pollFirst();
        count--;
        return element;
    }
}