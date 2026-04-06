package com.guiderag.chat.client;

import com.guiderag.common.dto.RetrievalRequestDTO;
import com.guiderag.common.dto.RetrievalResultDTO;
import com.guiderag.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 知识库服务降级处理
 * 当 rag-knowledge 服务不可用时返回空结果
 */
@Slf4j
@Component
public class KnowledgeServiceFallback implements KnowledgeServiceClient {

    @Override
    public Result<RetrievalResultDTO> retrieve(Long knowledgeBaseId, RetrievalRequestDTO request) {

        log.warn("[KnowledgeServiceFallback] 知识库服务不可用，降级返回空结果。knowledgeBaseId={}, query={}",
                knowledgeBaseId, request.getQuery());

        // 返回空结果，不中断对话流程
        return Result.success(RetrievalResultDTO.empty());
    }
}
