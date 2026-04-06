package com.guiderag.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "更新知识库请求参数")
@Data
public class KnowledgeBaseUpdateReqDTO {
    @Schema(description = "新的知识库名称（不传则不更新）", example = "新产品文档库")
    @Size(min = 1, max = 255, message = "知识库名称长度在 1-255 个字符之间")
    private String name;
    @Schema(description = "新的描述（不传则不更新）", example = "更新后的描述")
    private String description;
}
