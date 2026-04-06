package com.guiderag.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "知识库响应数据")
@Data
public class KnowledgeBaseResDTO {
    @Schema(description = "知识库 ID")
    private Long id;
    @Schema(description = "知识库名称")
    private String name;
    @Schema(description = "知识库描述")
    private String description;
    @Schema(description = "创建人用户 ID")
    private Long createdBy;
    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;
    @Schema(description = "最后修改时间")
    private LocalDateTime gmtModified;
    @Schema(description = "该知识库包含的文档数量")
    private Integer docCount;
}