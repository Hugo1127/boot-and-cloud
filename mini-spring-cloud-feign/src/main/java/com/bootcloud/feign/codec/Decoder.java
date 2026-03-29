package com.bootcloud.feign.codec;

/**
 * 解码器接口
 * 用于将 HTTP 响应体解码为 Java 对象
 */
public interface Decoder {
    /**
     * 将字符串解码为对象
     * @param content 要解码的内容
     * @param targetType 目标类型
     * @return 解码后的对象
     * @throws Exception 解码异常
     */
    <T> T decode(String content, Class<T> targetType) throws Exception;
}
