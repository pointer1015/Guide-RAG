package com.guiderag.knowledge.controller;

import com.guiderag.common.context.UserContextHolder;
import com.guiderag.common.dto.RetrievalRequestDTO;
import com.guiderag.common.dto.RetrievalResultDTO;
import com.guiderag.common.result.Result;
import com.guiderag.knowledge.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 向量检索控制器
 *
 * 提供知识库向量检索能力，供 rag-chat 服务调用
 * 核心功能：根据用户查询在知识库中检索相关文档片段
 *
 * @author Guide-RAG Team
 */
@Slf4j
@RestController
@RequestMapping("/rag/v1/knowledge-bases")
@RequiredArgsConstructor
@Tag(name = "向量检索", description = "知识库内容检索 API")
public class RetrievalController {

    private final RetrievalService retrievalService;

    /**
     * 知识库向量检索
     *
     * 执行流程:
     * 1. 用户查询文本向量化
     * 2. Milvus 相似度搜索
     * 3. 关联文档元数据
     * 4. 返回 TopK 相似文档片段
     *
     * @param knowledgeBaseId 知识库ID
     * @param request 检索请求参数
     * @return 检索结果列表
     */
    @PostMapping("/{knowledgeBaseId}/retrieve")
    @Operation(
            summary = "知识库向量检索",
            description = "根据查询文本在指定知识库中检索相关内容，返回相似度最高的文档片段。支持 RAG 问答场景。"
    )
    public Result<RetrievalResultDTO> retrieve(
            @Parameter(description = "知识库ID", required = true, example = "1001")
            @PathVariable Long knowledgeBaseId,

            @Parameter(description = "检索请求参数", required = true)
            @Valid @RequestBody RetrievalRequestDTO request
    ) {
        // 从上下文获取当前用户租户ID（网关注入）
        Long tenantId = UserContextHolder.getUserId();

        log.info("[RetrievalController] 检索请求: tenantId={}, kbId={}, query={}, topK={}",
                tenantId, knowledgeBaseId, request.getQuery(), request.getTopK());

        // 调用检索服务
        RetrievalResultDTO result = retrievalService.retrieve(tenantId, knowledgeBaseId, request);

        log.info("[RetrievalController] 检索完成: 返回 {} 条结果", result.getTotalCount());
        return Result.success(result);
    }
}
