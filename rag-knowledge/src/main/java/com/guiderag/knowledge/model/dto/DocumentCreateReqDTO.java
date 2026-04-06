package com.guiderag.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
@Schema(description = "创建文档请求")
public class DocumentCreateReqDTO {
    @Schema(description = "文档标题", example = "产品需求文档V1")
    private String title;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotBlank(message = "文件类型不能为空")
    private String fileType;

    @NotNull(message = "文件大小不能为空")
    @PositiveOrZero(message = "文件大小不能为负数")
    private Long fileSize;

    private String mimeType;

    @NotBlank(message = "存储桶不能为空")
    private String minioBucket;

    @NotBlank(message = "对象Key不能为空")
    private String minioObjectKey;

    @Schema(description = "内容哈希，建议SHA256")
    private String contentHash;
}
