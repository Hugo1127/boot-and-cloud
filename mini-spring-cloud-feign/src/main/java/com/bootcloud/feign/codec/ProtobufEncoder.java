package com.bootcloud.feign.codec;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Protobuf 编码器
 * 将 Java 对象或 Protobuf Message 编码为 Base64 字符串（便于 HTTP 传输）
 * 
 * 面试考点：
 * 1. 为什么 Protobuf 要使用 Base64 编码？
 *    答：Protobuf 是二进制格式，HTTP 文本协议需要 Base64 编码才能在文本协议中安全传输
 * 2. Protobuf 的优势？
 *    答：体积小（二进制编码）、性能高（无需反射解析）、强类型（schema 约束）
 * 3. Protobuf vs JSON 性能对比？
 *    答：Protobuf 体积通常是 JSON 的 1/3-1/10，序列化速度快 3-5 倍
 * 
 * 使用示例：
 * ```java
 * // 方式 1：直接编码 Protobuf Message
 * UserProtos.User user = UserProtos.User.newBuilder()
 *     .setId(1L)
 *     .setName("张三")
 *     .build();
 * String encoded = new ProtobufEncoder().encode(user);
 * 
 * // 方式 2：编码普通 Java 对象（内部转为 JSON 再转 Protobuf）
 * User user = new User(1L, "张三");
 * String encoded = new ProtobufEncoder().encode(user);
 * ```
 */
public class ProtobufEncoder implements Encoder {
    private static final Logger logger = LoggerFactory.getLogger(ProtobufEncoder.class);
    
    /**
     * 将对象编码为 Base64 字符串
     * 支持 Protobuf Message 和普通 Java 对象
     */
    @Override
    public String encode(Object object) throws Exception {
        if (object == null) {
            return null;
        }
        
        // 如果是字符串，直接返回
        if (object instanceof String) {
            return (String) object;
        }
        
        // 如果是 Protobuf Message，直接序列化
        if (object instanceof Message) {
            return encodeProtobufMessage((Message) object);
        }
        
        // 普通 Java 对象，使用 JSON 作为中间格式（简化实现）
        // 实际项目中应该为每个 DTO 定义对应的 Protobuf Message
        logger.warn("Encoding non-Protobuf object: {}. Consider using Protobuf Message for better performance.", 
                   object.getClass().getName());
        return encodeAsJson(object);
    }
    
    /**
     * 编码 Protobuf Message
     * 使用 Base64 编码便于 HTTP 传输
     */
    private String encodeProtobufMessage(Message message) throws IOException {
        logger.debug("Encoding Protobuf Message: {}", message.getDescriptorForType().getName());
        
        // 获取序列化后的大小
        int size = message.getSerializedSize();
        byte[] bytes = new byte[size];
        
        // 序列化到字节数组
        CodedOutputStream output = CodedOutputStream.newInstance(bytes);
        message.writeTo(output);
        output.checkNoSpaceLeft();
        
        // Base64 编码为字符串（便于 HTTP 传输）
        String base64 = Base64.getEncoder().encodeToString(bytes);
        
        logger.debug("Protobuf encoded size: {} bytes (JSON would be larger)", bytes.length);
        return base64;
    }
    
    /**
     * 编码普通 Java 对象为 JSON（降级方案）
     * 实际项目应该使用 Protobuf Message
     */
    private String encodeAsJson(Object object) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.writeValueAsString(object);
    }
}
