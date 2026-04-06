package com.guiderag.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量检索请求 DTO
 * 
 * 跨服务通用 DTO
 * 
 * @author Guide-RAG Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量检索请求")
public class RetrievalRequestDTO {

    @NotBlank(message = "查询文本不能为空")
    @Schema(description = "查询文本", required = true, example = "如何配置数据库连接?")
    private String query;

    @Min(value = 1, message = "topK 最小值为 1")
    @Max(value = 100, message = "topK 最大值为 100")
    @Schema(description = "返回结果数量", example = "5")
    private Integer topK;

    @Min(value = 0, message = "minScore 最小值为 0")
    @Max(value = 1, message = "minScore 最大值为 1")
    @Schema(description = "最低相似度阈值（0-1）", example = "0.6")
    private Float minScore;
}
