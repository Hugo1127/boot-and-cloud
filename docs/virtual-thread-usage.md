# 虚拟线程使用文档

## 1. 什么是虚拟线程

虚拟线程是 Java 21 引入的一种轻量级线程实现（本项目基于 JDK 25），它由 JVM 管理，而不是操作系统。虚拟线程具有以下特点：

- **轻量级**：虚拟线程的创建和切换成本远低于传统线程
- **高并发**：可以创建数百万个虚拟线程而不会耗尽系统资源
- **IO 密集型任务的理想选择**：虚拟线程在等待 IO 操作时会自动挂起，释放底层操作系统线程
- **向后兼容**：虚拟线程的 API 与传统线程完全兼容

## 2. 虚拟线程与传统线程的对比

| 特性 | 虚拟线程 | 传统线程 |
|------|---------|---------|
| 创建成本 | 极低 | 高 |
| 内存占用 | 几 KB | 几 MB |
| 并发能力 | 数百万 | 数千 |
| 切换成本 | 极低 | 高 |
| 适用场景 | IO 密集型任务 | CPU 密集型任务 |

## 3. 性能测试结果

我们在不同场景下对虚拟线程和传统线程进行了性能测试，结果如下：

### 3.1 高并发场景性能（100000 任务）
- 虚拟线程执行耗时: 105ms (952,381 任务/秒)
- 传统线程执行耗时: 1461ms (68,446 任务/秒)
- 性能提升: 92.81%

### 3.2 IO 密集型任务性能（10000 任务）
- 虚拟线程执行耗时: 38ms
- 传统线程执行耗时: 1576ms
- 性能提升: 97.59%

### 3.3 CPU 密集型任务性能（100 任务）
- 虚拟线程执行耗时: 207ms（更慢）
- 传统线程执行耗时: 52ms
- 性能提升: -298.08%（传统线程更优）

## 4. 使用方法

### 4.1 基本使用

#### 4.1.1 创建和启动虚拟线程

```java
// 使用 Thread.ofVirtual() 创建虚拟线程
Thread virtualThread = Thread.ofVirtual().start(() -> {
    System.out.println("Hello from virtual thread!");
    // 执行任务...
});

// 等待虚拟线程完成
virtualThread.join();
```

#### 4.1.2 创建命名的虚拟线程

```java
// 创建命名的虚拟线程
Thread namedThread = Thread.ofVirtual()
    .name("my-virtual-thread")
    .start(() -> {
        System.out.println("Hello from named virtual thread!");
        // 执行任务...
    });

namedThread.join();
```

### 4.2 使用 VirtualThreadUtils

`VirtualThreadUtils` 提供了更方便的虚拟线程创建和管理功能：

```java
// 创建虚拟线程
Thread thread = VirtualThreadUtils.createVirtualThread(() -> {
    System.out.println("Hello from virtual thread!");
    // 执行任务...
});

// 创建命名的虚拟线程
Thread namedThread = VirtualThreadUtils.createNamedVirtualThread("my-thread", () -> {
    System.out.println("Hello from named virtual thread!");
    // 执行任务...
});

// 使用构建器创建虚拟线程
Thread customThread = VirtualThreadUtils.VirtualThreadBuilder.builder()
    .name("custom-thread")
    .start(() -> {
        System.out.println("Hello from custom virtual thread!");
        // 执行任务...
    });
```

### 4.3 使用 VirtualThreadPool

`VirtualThreadPool` 提供了虚拟线程池功能，用于管理大量虚拟线程：

```java
// 创建虚拟线程池
VirtualThreadPool pool = VirtualThreadPool.create("my-virtual-pool");

// 提交任务
Future<?> future = pool.submit(() -> {
    System.out.println("Task executed in virtual thread pool");
    return "result";
});

// 执行任务
pool.execute(() -> {
    System.out.println("Task executed in virtual thread pool");
    // 执行任务...
});

// 批量执行任务
List<Runnable> tasks = new ArrayList<>();
for (int i = 0; i < 100; i++) {
    final int taskId = i;
    tasks.add(() -> {
        System.out.println("Task " + taskId + " executed");
        // 执行任务...
    });
}
pool.executeBatch(tasks);

// 打印线程池统计信息
pool.printStatistics();

// 关闭线程池
pool.shutdown();
```

### 4.4 使用 SmartVirtualThreadPool

`SmartVirtualThreadPool` 提供了与 `SmartThreadPool` 类似的接口，方便用户在传统线程和虚拟线程之间切换：

```java
// 创建智能虚拟线程池
SmartVirtualThreadPool pool = SmartVirtualThreadPool.createVirtualThreadPool("smart-virtual-pool");

// 提交任务
Future<?> future = pool.submit(() -> {
    System.out.println("Task executed in smart virtual thread pool");
    return "result";
});

// 执行任务
pool.execute(() -> {
    System.out.println("Task executed in smart virtual thread pool");
    // 执行任务...
});

// 打印线程池统计信息
pool.printStatistics();
```

### 4.5 使用 ThreadPoolFactory

`SmartVirtualThreadPool.ThreadPoolFactory` 提供了统一的线程池创建接口，可以根据任务类型创建合适的线程池：

```java
// 创建 CPU 密集型线程池
ExecutorService cpuPool = SmartVirtualThreadPool.ThreadPoolFactory.createThreadPool("cpu-pool", SmartVirtualThreadPool.TaskType.CPU_INTENSIVE);

// 创建 IO 密集型线程池
ExecutorService ioPool = SmartVirtualThreadPool.ThreadPoolFactory.createThreadPool("io-pool", SmartVirtualThreadPool.TaskType.IO_INTENSIVE);

// 创建虚拟线程池
ExecutorService virtualPool = SmartVirtualThreadPool.ThreadPoolFactory.createThreadPool("virtual-pool", SmartVirtualThreadPool.TaskType.VIRTUAL);
```

## 5. 最佳实践

### 5.1 适用场景

- **IO 密集型任务**：网络请求、数据库操作、文件读写等
- **高并发场景**：需要处理大量并发请求的场景
- **任务数量多但每个任务执行时间短的场景**

### 5.2 不适用场景

- **CPU 密集型任务**：虽然虚拟线程在 CPU 密集型任务上也有一定性能提升，但传统线程池可能更适合
- **需要长时间占用 CPU 的任务**

### 5.3 注意事项

1. **避免阻塞操作**：虽然虚拟线程在等待 IO 时会自动挂起，但在等待锁或其他同步操作时仍然会阻塞
2. **合理设置线程池大小**：虚拟线程池不需要设置大小限制，因为虚拟线程是轻量级的
3. **监控和管理**：使用 `printStatistics()` 方法监控虚拟线程池的运行状态
4. **异常处理**：确保在虚拟线程中正确处理异常，避免未捕获的异常导致应用程序崩溃

## 6. 示例代码

### 6.1 基本示例

```java
import com.bootcloud.concurrent.VirtualThreadUtils;

public class VirtualThreadExample {
    public static void main(String[] args) throws InterruptedException {
        // 创建并启动虚拟线程
        Thread thread = VirtualThreadUtils.createVirtualThread(() -> {
            System.out.println("Hello from virtual thread!");
            try {
                Thread.sleep(1000); // 模拟 IO 等待
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Virtual thread finished");
        });

        // 等待虚拟线程完成
        thread.join();
        System.out.println("Main thread finished");
    }
}
```

### 6.2 高并发示例

```java
import com.bootcloud.concurrent.VirtualThreadPool;

import java.util.concurrent.CountDownLatch;

public class HighConcurrencyExample {
    public static void main(String[] args) throws InterruptedException {
        int taskCount = 100000;
        CountDownLatch latch = new CountDownLatch(taskCount);

        // 创建虚拟线程池
        VirtualThreadPool pool = VirtualThreadPool.create("high-concurrency-pool");

        long start = System.currentTimeMillis();

        // 提交大量任务
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            pool.execute(() -> {
                try {
                    // 模拟简单任务
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有任务完成
        latch.await();

        long end = System.currentTimeMillis();
        System.out.println("执行 " + taskCount + " 个任务耗时: " + (end - start) + "ms");
        System.out.println("每秒处理任务数: " + (taskCount * 1000.0 / (end - start)));

        // 打印线程池统计信息
        pool.printStatistics();

        // 关闭线程池
        pool.shutdown();
    }
}
```

### 6.3 Web 服务示例

```java
import com.bootcloud.concurrent.SmartVirtualThreadPool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class VirtualThreadWebServer {
    public static void main(String[] args) throws IOException {
        // 创建虚拟线程池
        SmartVirtualThreadPool pool = SmartVirtualThreadPool.createVirtualThreadPool("web-server-pool");

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server started on port 8080");

            while (true) {
                Socket socket = serverSocket.accept();
                // 使用虚拟线程处理每个客户端连接
                pool.execute(() -> handleClient(socket));
            }
        }
    }

    private static void handleClient(Socket socket) {
        try {
            // 处理客户端请求
            System.out.println("Handling client connection from " + socket.getInetAddress());
            // 模拟处理时间
            Thread.sleep(100);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 7. 总结

虚拟线程是 Java 21 引入的一项重大特性（本项目基于 JDK 25），它为高并发场景提供了一种更高效的线程实现方式。通过使用虚拟线程，我们可以：

- 显著提高 IO 密集型任务的性能
- 支持更高的并发度
- 减少系统资源消耗
- 简化并发编程模型

在适当的场景下使用虚拟线程，可以为应用程序带来显著的性能提升和资源利用优化。