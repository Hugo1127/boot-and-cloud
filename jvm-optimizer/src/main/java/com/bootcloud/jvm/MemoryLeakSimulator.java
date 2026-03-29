package com.bootcloud.jvm;

import java.util.ArrayList;
import java.util.List;

public class MemoryLeakSimulator {
    private static final List<byte[]> memoryLeakList = new ArrayList<>();

    public static void simulateMemoryLeak() {
        System.out.println("Starting memory leak simulation...");
        int count = 0;
        try {
            while (true) {
                byte[] bytes = new byte[1024 * 1024];
                memoryLeakList.add(bytes);
                count++;
                
                if (count % 100 == 0) {
                    System.out.println("Allocated " + count + " MB, heap usage: " + 
                        JVMInfo.getMemoryInfo().get("heap.usagePercent") + "%");
                    Thread.sleep(100);
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError caught after allocating " + count + " MB");
            throw e;
        } catch (InterruptedException e) {
            System.out.println("Memory leak simulation interrupted");
        }
    }

    public static void simulateNormalUsage() {
        System.out.println("Simulating normal memory usage...");
        for (int i = 0; i < 1000; i++) {
            byte[] bytes = new byte[1024 * 1024];
            processBytes(bytes);
        }
        System.out.println("Normal usage completed");
    }

    private static void processBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i % 256);
        }
    }

    public static void clearLeak() {
        System.out.println("Clearing memory leak list...");
        memoryLeakList.clear();
        System.out.println("Memory leak list cleared");
    }
}
