package com.bootcloud.feign.codec;

/**
 * 编码器接口
 * 用于将 Java 对象编码为 HTTP 请求体
 * 
 * 面试考点：
 * 1. 常见的序列化协议有哪些？
 *    答：JSON、XML、Protobuf、Thrift 等
 * 2. JSON 和 Protobuf 的区别？
 *    答：JSON 是文本格式，人类可读但体积大；Protobuf 是二进制格式，体积小、性能高但需要 schema
 */
public interface Encoder {
    /**
     * 将对象编码为字符串
     * @param object 要编码的对象
     * @return 编码后的字符串
     * @throws Exception 编码异常
     */
    String encode(Object object) throws Exception;
}
