package com.guiderag.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档解析状态响应 DTO
 */
@Data
@Schema(description = "文档解析状态响应")
public class DocumentStatusResDTO {

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "解析状态: PENDING-排队中, PARSING-解析中, PARSED-成功, FAILED-失败")
    private String status;

    @Schema(description = "处理进度 (0-100)")
    private Integer progress;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "总分块数")
    private Integer chunkCount;

    @Schema(description = "最后修改时间")
    private LocalDateTime gmtModified;
}
