package com.guiderag.chat.client;

import com.guiderag.common.dto.RetrievalRequestDTO;
import com.guiderag.common.dto.RetrievalResultDTO;
import com.guiderag.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 知识库服务 Feign 客户端
 * 调用 rag-knowledge 服务进行向量检索
 */
@FeignClient(
        name = "rag-knowledge",
        path = "/rag/v1",
        fallback = KnowledgeServiceFallback.class
)
public interface KnowledgeServiceClient {

    /**
     * 向量检索接口
     *
     * @param knowledgeBaseId 知识库 ID
     * @param request 检索请求
     * @return 检索结果
     */
    @PostMapping("/knowledge-bases/{knowledgeBaseId}/retrieve")
    Result<RetrievalResultDTO> retrieve(
            @PathVariable("knowledgeBaseId") Long knowledgeBaseId,
            @RequestBody RetrievalRequestDTO request
    );
}
