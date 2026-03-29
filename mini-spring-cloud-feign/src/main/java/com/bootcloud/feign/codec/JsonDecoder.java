package com.bootcloud.feign.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON 解码器
 * 基于 Jackson 实现 JSON 到对象的解码
 */
public class JsonDecoder implements Decoder {
    private static final Logger logger = LoggerFactory.getLogger(JsonDecoder.class);
    private final ObjectMapper objectMapper;
    
    public JsonDecoder() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public <T> T decode(String content, Class<T> targetType) throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        logger.debug("Decoding JSON to object: {}", targetType.getName());
        return objectMapper.readValue(content, targetType);
    }
}
