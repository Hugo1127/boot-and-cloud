package com.bootcloud.feign.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON 编码器
 * 基于 Jackson 实现对象到 JSON 的编码
 */
public class JsonEncoder implements Encoder {
    private static final Logger logger = LoggerFactory.getLogger(JsonEncoder.class);
    private final ObjectMapper objectMapper;
    
    public JsonEncoder() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String encode(Object object) throws Exception {
        if (object == null) {
            return null;
        }
        if (object instanceof String) {
            return (String) object;
        }
        logger.debug("Encoding object to JSON: {}", object.getClass().getName());
        return objectMapper.writeValueAsString(object);
    }
}
