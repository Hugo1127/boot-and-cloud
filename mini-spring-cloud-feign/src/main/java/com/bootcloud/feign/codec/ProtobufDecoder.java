package com.bootcloud.feign.codec;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Protobuf 解码器
 * 将 Base64 字符串解码为 Protobuf Message 或 Java 对象
 * 
 * 面试考点：
 * 1. Protobuf 解码的核心原理？
 *    答：根据 schema 定义的字段编号和类型，解析二进制数据
 * 2. DynamicMessage 的作用？
 *    答：无需生成 Java 类，根据 Descriptor 动态创建 Message 实例
 * 3. Protobuf 的向后兼容性？
 *    答：通过字段编号识别，新增字段不影响旧版本解析
 * 
 * 使用示例：
 * ```java
 * ProtobufDecoder decoder = new ProtobufDecoder();
 * 
 * // 解码为 Protobuf Message
 * Message message = decoder.decode(base64String, UserProtos.User.class);
 * 
 * // 解码为普通 Java 对象（需要提供 Descriptor）
 * DynamicMessage dm = decoder.decode(base64String, descriptor);
 * ```
 */
public class ProtobufDecoder implements Decoder {
    private static final Logger logger = LoggerFactory.getLogger(ProtobufDecoder.class);
    
    /**
     * 将 Base64 字符串解码为 Java 对象
     * 注意：由于类型擦除，需要额外提供 Protobuf Message 的默认实例
     */
    @Override
    public <T> T decode(String content, Class<T> targetType) throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        // 如果是字符串类型，直接返回
        if (targetType == String.class) {
            return targetType.cast(content);
        }
        
        // 尝试 Base64 解码
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException e) {
            // 不是 Base64，可能是 JSON，降级处理
            logger.warn("Content is not Base64, attempting JSON fallback");
            return decodeAsJson(content, targetType);
        }
        
        // 如果目标是 Protobuf Message 类
        if (com.google.protobuf.Message.class.isAssignableFrom(targetType)) {
            return decodeProtobufMessage(bytes, targetType);
        }
        
        // 普通 Java 对象，使用 JSON 解码
        logger.warn("Decoding to non-Protobuf type: {}. Consider using Protobuf Message for better performance.", 
                   targetType.getName());
        return decodeAsJson(content, targetType);
    }
    
    /**
     * 解码 Protobuf Message
     * 使用反射创建 Message 实例
     */
    @SuppressWarnings("unchecked")
    private <T> T decodeProtobufMessage(byte[] bytes, Class<T> targetType) throws Exception {
        logger.debug("Decoding Protobuf Message: {}", targetType.getName());
        
        // 获取 Message 的默认实例（通过静态方法 getDefaultInstance）
        com.google.protobuf.Message defaultInstance = getDefaultInstance(targetType);
        
        if (defaultInstance == null) {
            throw new IllegalArgumentException("Cannot get default instance for: " + targetType.getName());
        }
        
        // 使用 DynamicMessage 解析
        CodedInputStream input = CodedInputStream.newInstance(bytes);
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(
            defaultInstance.getDescriptorForType(), 
            input
        );
        
        // 转换为目标类型（通过 toBuilder 和 build）
        com.google.protobuf.Message.Builder builder = defaultInstance.newBuilderForType();
        builder.mergeFrom(dynamicMessage);
        
        logger.debug("Protobuf decoded size: {} bytes", bytes.length);
        return (T) builder.build();
    }
    
    /**
     * 获取 Protobuf Message 的默认实例
     * 使用反射调用静态方法 getDefaultInstance(Class)
     */
    @SuppressWarnings("unchecked")
    private com.google.protobuf.Message getDefaultInstance(Class<?> clazz) {
        try {
            java.lang.reflect.Method method = clazz.getMethod("getDefaultInstance");
            return (com.google.protobuf.Message) method.invoke(null);
        } catch (Exception e) {
            logger.error("Failed to get default instance for {}", clazz.getName(), e);
            return null;
        }
    }
    
    /**
     * 解码为 JSON（降级方案）
     */
    private <T> T decodeAsJson(String content, Class<T> targetType) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readValue(content, targetType);
    }
}
