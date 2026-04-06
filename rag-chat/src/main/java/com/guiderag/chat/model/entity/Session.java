package com.guiderag.chat.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 会话实体类
 * 对应数据库 session 表
 */
@Data
public class Session {

    /** 会话ID（雪花ID） */
    private Long id;

    /** 归属租户ID */
    private Long tenantId;

    /** 创建用户ID */
    private Long userId;

    /** 关联知识库ID（可为空，空表示全局检索） */
    private Long knowledgeBaseId;

    /** 会话标题 */
    private String title;

    /** 最后一条消息时间 */
    private OffsetDateTime lastMessageAt;

    /** 创建时间 */
    private LocalDateTime gmtCreate;

    /** 更新时间 */
    private LocalDateTime gmtModified;

    /** 软删除标志：0-正常，1-已删除 */
    private Integer isDeleted;
}
