package com.bootcloud.feign.codec;

/**
 * 编码器接口
 * 用于将 Java 对象编码为 HTTP 请求体
 * 
 * 面试考点：
 * 1. 常见的序列化协议有哪些？
 *    答：JSON、XML、Protobuf、Thrift、Avro、Hessian 等
 * 
 * 2. JSON 和 Protobuf 的核心区别？
 *    答：
 *    - JSON：文本格式，人类可读，基于键值对，体积较大，解析慢（需要反射）
 *    - Protobuf：二进制格式，机器高效，基于字段编号，体积小（1/3-1/10），解析快（3-5 倍）
 * 
 * 3. 序列化协议选型原则？
 *    答：
 *    - 对外 API：JSON（可读性好，通用性强）
 *    - 内部 RPC：Protobuf/Thrift（性能优先）
 *    - 大数据场景：Avro（支持 Schema 演化）
 *    - Java 生态：Hessian（二进制，Java 原生支持）
 * 
 * 4. Protobuf 的核心优势？
 *    答：
 *    - 性能：序列化速度快 3-5 倍，反序列化快 3-5 倍
 *    - 体积：二进制编码，体积是 JSON 的 1/3-1/10
 *    - 兼容性：支持向前/向后兼容（字段编号机制）
 *    - 强类型：通过 .proto schema 约束
 * 
 * 5. Protobuf 的缺点？
 *    答：
 *    - 需要预定义 schema（.proto 文件）
 *    - 人类不可读（二进制格式）
 *    - 学习成本（需要编译生成代码）
 *    - 灵活性差（修改 schema 需要重新编译）
 * 
 * 6. Base64 编码的作用和开销？
 *    答：
 *    - 作用：将二进制数据转换为文本格式，便于 HTTP 传输
 *    - 开销：数据膨胀约 33%（4 个字节表示 3 个字节）
 *    - 优化：使用 HTTP/2 或 gRPC 直接传输二进制
 * 
 * 7. 什么是 Schema 演化（Schema Evolution）？
 *    答：在不破坏兼容性的前提下修改数据结构
 *    - 新增字段：必须设置可选（optional），有默认值
 *    - 删除字段：不能重用字段编号，保留编号
 *    - 修改类型：必须是兼容的类型（如 int32→int64）
 * 
 * 8. Protobuf 的字段编号规则？
 *    答：
 *    - 字段编号从 1 开始，不能重复
 *    - 1-15 占 1 字节（包含编号和类型），16-2047 占 2 字节
 *    - 建议：频繁使用的字段使用 1-15 的编号
 *    - 保留编号：deleted_field = 99 [deprecated = true];
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
