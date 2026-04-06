package com.guiderag.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "创建知识库请求参数")
@Data
public class KnowledgeBaseCreateReqDTO {
    @Schema(description = "知识库名称（同一用户下不可重复）", example = "产品文档库")
    @NotBlank(message = "知识库名称不能为空")
    @Size(min = 1, max = 255, message = "知识库名称长度在 1-255 个字符之间")
    private String name;
    @Schema(description = "知识库描述（可选）", example = "存放所有产品相关的文档和手册")
    private String description;
}