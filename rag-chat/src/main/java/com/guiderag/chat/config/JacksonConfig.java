package com.guiderag.chat.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局序列化配置
 *
 * 问题背景：
 * Java Long 类型为 64 位整数，最大值约为 9.2×10^18。
 * 而 JavaScript Number 仅能精确表示 53 位整数（Number.MAX_SAFE_INTEGER = 2^53-1 ≈ 9×10^15）。
 * 雪花算法生成的 ID（如 2040439175731855360）超出此范围，
 * 被 JSON.parse 解析为 JS number 时会发生精度丢失（变成 2040439175731855400），
 * 导致前端用错误的 ID 请求后端，引发"会话不存在"等异常。
 *
 * 解决方案：
 * 将所有 Long/long 类型在 JSON 序列化时输出为字符串（加引号），
 * 前端以字符串形式使用 ID，完全规避精度问题。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringSerializer() {
        return builder -> {
            // Long 包装类 → "123456789" (字符串)
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            // long 基本类型 → "123456789" (字符串)
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
