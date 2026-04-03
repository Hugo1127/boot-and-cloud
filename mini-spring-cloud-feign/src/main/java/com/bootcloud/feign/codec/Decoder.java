package com.bootcloud.feign.codec;

/**
 * 解码器接口
 * 用于将 HTTP 响应体解码为 Java 对象
 * 
 * 面试考点：
 * 1. 反序列化的安全隐患？
 *    答：
 *    - 类型混淆攻击：攻击者构造恶意数据实例化任意类
 *    - 解决方案：白名单机制，只允许反序列化可信类
 *    - 案例：Jackson 默认允许任意类，需配置 MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES
 * 
 * 2. JSON 反序列化的性能瓶颈？
 *    答：
 *    - 反射开销：需要反射创建对象、设置字段
 *    - 优化方案：
 *      * 使用字段缓存（Field Cache）
 *      * 使用代码生成（如 Jackson Afterburner）
 *      * 使用 Unsafe 直接内存操作（风险高）
 * 
 * 3. Protobuf 反序列化的优势？
 *    答：
 *    - 无需反射：根据预生成的代码直接访问字段
 *    - 流式解析：边读边解析，无需加载整个对象
 *    - 零拷贝：可直接使用字节数组，避免复制
 * 
 * 4. 如何处理版本兼容性？
 *    答：
 *    - JSON：通过 @JsonIgnoreProperties 忽略未知字段
 *    - Protobuf：字段编号机制，自动忽略未知编号
 *    - 通用方案：版本号字段 + 多版本兼容逻辑
 * 
 * 5. 泛型反序列化的问题？
 *    答：
 *    - 类型擦除：运行时无法获取泛型类型
 *    - 解决方案：
 *      * TypeReference（Jackson）
 *      * ParameterizedTypeImpl
 *      * 传入 Class 参数
 * 
 * 6. 大对象反序列化优化？
 *    答：
 *    - 流式解析：SAX（XML）、流式 JSON 解析
 *    - 分块处理：大文件分块读取
 *    - 懒加载：按需加载字段
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
