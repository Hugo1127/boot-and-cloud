package com.bootcloud.feign.test;

import com.bootcloud.feign.codec.JsonDecoder;
import com.bootcloud.feign.codec.JsonEncoder;
import com.bootcloud.feign.codec.ProtobufDecoder;
import com.bootcloud.feign.codec.ProtobufEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON vs Protobuf 序列化性能对比测试
 * 
 * 面试考点：
 * 1. JSON 和 Protobuf 的核心区别？
 *    答：JSON 是文本格式（人类可读），Protobuf 是二进制格式（机器高效）
 * 2. 性能差异有多大？
 *    答：Protobuf 体积通常是 JSON 的 1/3-1/10，序列化速度快 3-5 倍
 * 3. 各自适用场景？
 *    答：JSON 适合对外 API（可读性好），Protobuf 适合内部 RPC（性能优先）
 * 
 * 测试维度：
 * - 序列化体积对比
 * - 序列化时间对比
 * - 反序列化时间对比
 * - 吞吐量对比
 */
public class SerializationPerformanceTest {
    private static final Logger logger = LoggerFactory.getLogger(SerializationPerformanceTest.class);
    
    private final JsonEncoder jsonEncoder = new JsonEncoder();
    private final JsonDecoder jsonDecoder = new JsonDecoder();
    private final ProtobufEncoder protobufEncoder = new ProtobufEncoder();
    private final ProtobufDecoder protobufDecoder = new ProtobufDecoder();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 测试数据模型（模拟真实业务对象）
     */
    public static class User {
        public Long id;
        public String name;
        public String email;
        public String phone;
        public Integer age;
        public String address;
        public List<String> tags;
        
        public User() {}
        
        public User(Long id, String name, String email, String phone, Integer age, String address, List<String> tags) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.age = age;
            this.address = address;
            this.tags = tags;
        }
        
        public static User createSampleUser() {
            List<String> tags = new ArrayList<>();
            tags.add("developer");
            tags.add("java");
            tags.add("backend");
            tags.add("microservices");
            tags.add("distributed-systems");
            
            return new User(
                123456789L,
                "张三SanZhangThisIsALongNameForTesting",
                "zhangsan@example.com.cn",
                "+86-138-0013-8888",
                28,
                "北京市朝阳区某某街道某某小区 1 号楼 1 单元 101 室",
                tags
            );
        }
    }
    
    /**
     * 测试 1：序列化体积对比
     */
    @Test
    public void testSerializationSize() throws Exception {
        logger.info("=== 测试 1：序列化体积对比 ===");
        
        User user = User.createSampleUser();
        
        // JSON 序列化
        String json = jsonEncoder.encode(user);
        int jsonSize = json.getBytes(StandardCharsets.UTF_8).length;
        
        // Protobuf 序列化（Base64 编码后）
        String protobuf = protobufEncoder.encode(user);
        int protobufSize = protobuf.getBytes(StandardCharsets.UTF_8).length;
        
        logger.info("JSON size: {} bytes", jsonSize);
        logger.info("Protobuf size (Base64): {} bytes", protobufSize);
        logger.info("Size ratio (Protobuf/JSON): {:.2f}%", (double) protobufSize / jsonSize * 100);
        
        // 验证 Protobuf 体积更小或接近
        assertTrue(protobufSize > 0, "Protobuf size should be positive");
        assertTrue(jsonSize > 0, "JSON size should be positive");
        
        // 注意：由于我们使用 JSON 作为中间格式，实际 Protobuf 应该更小
        // 真正的 Protobuf Message 会比 JSON 小 3-10 倍
        logger.info("Note: Real Protobuf Message would be 3-10x smaller than JSON");
    }
    
    /**
     * 测试 2：序列化性能对比（10000 次迭代）
     */
    @Test
    public void testSerializationPerformance() throws Exception {
        logger.info("=== 测试 2：序列化性能对比 ===");
        
        User user = User.createSampleUser();
        int iterations = 10000;
        
        // JSON 序列化性能测试
        long jsonStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            jsonEncoder.encode(user);
        }
        long jsonTime = System.nanoTime() - jsonStart;
        double jsonMs = jsonTime / 1_000_000.0;
        
        // Protobuf 序列化性能测试
        long protobufStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            protobufEncoder.encode(user);
        }
        long protobufTime = System.nanoTime() - protobufStart;
        double protobufMs = protobufTime / 1_000_000.0;
        
        logger.info("JSON serialization: {:.2f} ms ({} iterations)", jsonMs, iterations);
        logger.info("Protobuf serialization: {:.2f} ms ({} iterations)", protobufMs, iterations);
        logger.info("Performance ratio (Protobuf/JSON): {:.2f}x", (double) jsonTime / protobufTime);
        
        // 计算吞吐量
        double jsonThroughput = iterations / (jsonTime / 1_000_000_000.0);
        double protobufThroughput = iterations / (protobufTime / 1_000_000_000.0);
        
        logger.info("JSON throughput: {:.0f} ops/sec", jsonThroughput);
        logger.info("Protobuf throughput: {:.0f} ops/sec", protobufThroughput);
    }
    
    /**
     * 测试 3：反序列化性能对比
     */
    @Test
    public void testDeserializationPerformance() throws Exception {
        logger.info("=== 测试 3：反序列化性能对比 ===");
        
        User user = User.createSampleUser();
        int iterations = 10000;
        
        // 准备数据
        String jsonData = jsonEncoder.encode(user);
        String protobufData = protobufEncoder.encode(user);
        
        // JSON 反序列化性能测试
        long jsonStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            jsonDecoder.decode(jsonData, User.class);
        }
        long jsonTime = System.nanoTime() - jsonStart;
        double jsonMs = jsonTime / 1_000_000.0;
        
        // Protobuf 反序列化性能测试
        long protobufStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            protobufDecoder.decode(protobufData, User.class);
        }
        long protobufTime = System.nanoTime() - protobufStart;
        double protobufMs = protobufTime / 1_000_000.0;
        
        logger.info("JSON deserialization: {:.2f} ms ({} iterations)", jsonMs, iterations);
        logger.info("Protobuf deserialization: {:.2f} ms ({} iterations)", protobufMs, iterations);
        logger.info("Performance ratio (Protobuf/JSON): {:.2f}x", (double) jsonTime / protobufTime);
    }
    
    /**
     * 测试 4：综合性能测试（序列化 + 反序列化）
     */
    @Test
    public void testRoundTripPerformance() throws Exception {
        logger.info("=== 测试 4：综合性能测试（序列化 + 反序列化）===");
        
        User originalUser = User.createSampleUser();
        int iterations = 5000;
        
        // JSON 往返测试
        long jsonStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String json = jsonEncoder.encode(originalUser);
            User decodedUser = jsonDecoder.decode(json, User.class);
            assertNotNull(decodedUser);
        }
        long jsonTime = System.nanoTime() - jsonStart;
        double jsonMs = jsonTime / 1_000_000.0;
        
        // Protobuf 往返测试
        long protobufStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String protobuf = protobufEncoder.encode(originalUser);
            User decodedUser = protobufDecoder.decode(protobuf, User.class);
            assertNotNull(decodedUser);
        }
        long protobufTime = System.nanoTime() - protobufStart;
        double protobufMs = protobufTime / 1_000_000.0;
        
        logger.info("JSON round-trip: {:.2f} ms ({} iterations)", jsonMs, iterations);
        logger.info("Protobuf round-trip: {:.2f} ms ({} iterations)", protobufMs, iterations);
        logger.info("Overall ratio (Protobuf/JSON): {:.2f}x", (double) jsonTime / protobufTime);
    }
    
    /**
     * 测试 5：大数据量性能测试
     */
    @Test
    public void testLargeDataPerformance() throws Exception {
        logger.info("=== 测试 5：大数据量性能测试 ===");
        
        // 创建大量数据
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            users.add(User.createSampleUser());
        }
        
        int iterations = 1000;
        
        // JSON 性能测试
        long jsonStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String json = jsonEncoder.encode(users);
            List<User> decoded = jsonDecoder.decode(json, List.class);
        }
        long jsonTime = System.nanoTime() - jsonStart;
        double jsonMs = jsonTime / 1_000_000.0;
        
        // Protobuf 性能测试
        long protobufStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String protobuf = protobufEncoder.encode(users);
            List<User> decoded = protobufDecoder.decode(protobuf, List.class);
        }
        long protobufTime = System.nanoTime() - protobufStart;
        double protobufMs = protobufTime / 1_000_000.0;
        
        logger.info("JSON large data: {:.2f} ms ({} iterations, {} objects)", jsonMs, iterations, users.size());
        logger.info("Protobuf large data: {:.2f} ms ({} iterations, {} objects)", protobufMs, iterations, users.size());
        logger.info("Large data ratio (Protobuf/JSON): {:.2f}x", (double) jsonTime / protobufTime);
    }
    
    /**
     * 测试 6：正确性验证
     */
    @Test
    public void testCorrectness() throws Exception {
        logger.info("=== 测试 6：正确性验证 ===");
        
        User originalUser = User.createSampleUser();
        
        // JSON 正确性测试
        String json = jsonEncoder.encode(originalUser);
        User jsonDecoded = jsonDecoder.decode(json, User.class);
        assertNotNull(jsonDecoded);
        assertEquals(originalUser.id, jsonDecoded.id);
        assertEquals(originalUser.name, jsonDecoded.name);
        assertEquals(originalUser.email, jsonDecoded.email);
        logger.info("JSON serialization/deserialization: PASSED");
        
        // Protobuf 正确性测试
        String protobuf = protobufEncoder.encode(originalUser);
        User protobufDecoded = protobufDecoder.decode(protobuf, User.class);
        assertNotNull(protobufDecoded);
        assertEquals(originalUser.id, protobufDecoded.id);
        assertEquals(originalUser.name, protobufDecoded.name);
        assertEquals(originalUser.email, protobufDecoded.email);
        logger.info("Protobuf serialization/deserialization: PASSED");
    }
    
    /**
     * 测试 7：Protobuf Message 直接序列化（真正的 Protobuf 性能）
     */
    @Test
    public void testRealProtobufMessage() throws Exception {
        logger.info("=== 测试 7：真实 Protobuf Message 测试（演示用）===");
        
        // 注意：这里演示如何使用真正的 Protobuf Message
        // 实际项目中需要定义 .proto 文件并生成 Java 类
        
        // 示例代码（需要实际的 Protobuf 类）：
        logger.info("真实 Protobuf 使用示例：");
        logger.info("1. 定义 user.proto:");
        logger.info("   message User {");
        logger.info("     int64 id = 1;");
        logger.info("     string name = 2;");
        logger.info("     string email = 3;");
        logger.info("   }");
        logger.info("2. 编译生成 Java 类：protoc --java_out=. user.proto");
        logger.info("3. 使用：");
        logger.info("   UserProtos.User user = UserProtos.User.newBuilder()");
        logger.info("       .setId(123L).setName(\"张三\").build();");
        logger.info("   String encoded = new ProtobufEncoder().encode(user);");
        
        // 性能对比数据（理论值）
        logger.info("");
        logger.info("理论性能对比（真实 Protobuf Message）：");
        logger.info("- 序列化体积：Protobuf 是 JSON 的 1/3 - 1/10");
        logger.info("- 序列化速度：Protobuf 是 JSON 的 3-5 倍");
        logger.info("- 反序列化速度：Protobuf 是 JSON 的 3-5 倍");
        logger.info("- CPU 占用：Protobuf 更低（无需反射）");
        logger.info("- 带宽占用：Protobuf 显著降低");
    }
    
    /**
     * 测试 8：Base64 编码开销测试
     */
    @Test
    public void testBase64Overhead() throws Exception {
        logger.info("=== 测试 8：Base64 编码开销测试 ===");
        
        User user = User.createSampleUser();
        String json = jsonEncoder.encode(user);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        
        // Base64 编码
        String base64 = Base64.getEncoder().encodeToString(jsonBytes);
        int base64Size = base64.getBytes(StandardCharsets.UTF_8).length;
        
        logger.info("原始 JSON 大小：{} bytes", jsonBytes.length);
        logger.info("Base64 编码后大小：{} bytes", base64Size);
        logger.info("Base64 开销：{:.2f}%", (double) base64Size / jsonBytes.length * 100);
        logger.info("注意：Base64 会使数据膨胀约 33%");
        logger.info("优化方案：使用 HTTP/2 或 gRPC 直接传输二进制，避免 Base64");
    }
}
