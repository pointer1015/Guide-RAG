package com.guiderag.knowledge.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBase {

    /** 知识库 ID（雪花算法生成，非自增） */
    private Long id;
    /** 归属租户 ID（MVP 阶段与 userId 相同） */
    private Long tenantId;
    /** 知识库名称（同一租户下唯一） */
    private String name;
    /** 知识库描述（可为空） */
    private String description;
    /** 创建人 userId */
    private Long createdBy;
    /** 乐观锁版本号（后续做并发控制用） */
    private Integer version;
    /** 创建时间 */
    private LocalDateTime gmtCreate;
    /** 最后修改时间 */
    private LocalDateTime gmtModified;
    /** 软删除标志（0：正常，1：已删除） */
    private Integer isDeleted;
}
