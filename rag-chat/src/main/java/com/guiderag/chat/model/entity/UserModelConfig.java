package com.guiderag.chat.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户自定义模型配置实体
 * 每个用户只有一条配置记录（tenant_id + user_id 唯一约束）
 */
@Data
public class UserModelConfig {

    /** 主键 */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 用户ID */
    private Long userId;

    /** 模型提供商标识 (openai/deepseek/gemini/ollama/custom) */
    private String provider;

    /** API Key */
    private String apiKey;

    /** API Base URL */
    private String baseUrl;

    /** 模型名称 */
    private String model;

    /** 是否启用: 1-启用, 0-禁用(回退默认模型) */
    private Integer isActive;

    /** 创建时间 */
    private LocalDateTime gmtCreate;

    /** 更新时间 */
    private LocalDateTime gmtModified;
}
