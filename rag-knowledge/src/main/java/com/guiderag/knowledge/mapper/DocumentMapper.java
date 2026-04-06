package com.guiderag.knowledge.mapper;

import com.guiderag.knowledge.model.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DocumentMapper {

    int insert(Document record);

    Document selectById(@Param("id") Long id,
                        @Param("knowledgeBaseId") Long knowledgeBaseId,
                        @Param("tenantId") Long tenantId);

    List<Document> selectByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId,
                                           @Param("tenantId") Long tenantId);


    Long countByKbAndHash(@Param("knowledgeBaseId") Long knowledgeBaseId,
                          @Param("tenantId") Long tenantId,
                          @Param("contentHash") String contentHash);

    int deleteById(@Param("id") Long id,
                   @Param("knowledgeBaseId") Long knowledgeBaseId,
                   @Param("tenantId") Long tenantId);


    /**
     * 仅通过ID查询文档（不校验租户，内部使用）
     * 用途：异步任务中查询文档元数据
     */
    Document selectByIdOnly(@Param("id") Long id);

    /**
     * 更新解析状态
     * 用途：解析开始时设置为 PARSING
     */
    int updateParseStatus(@Param("id") Long id,
                          @Param("parseStatus") String parseStatus,
                          @Param("parseError") String parseError);

    /**
     * 解析成功时更新状态
     * 用途：解析完成后更新状态为 PARSED，记录分块数量和完成时间
     */
    int updateParseStatusSuccess(@Param("id") Long id,
                                 @Param("parseStatus") String parseStatus,
                                 @Param("chunkCount") Integer chunkCount,
                                 @Param("parsedAt") LocalDateTime parsedAt);

    /**
     * 解析失败时更新状态
     * 用途：解析失败后更新状态为 FAILED，记录错误信息和重试次数
     */
    int updateParseStatusFailed(@Param("id") Long id,
                                @Param("parseStatus") String parseStatus,
                                @Param("parseError") String parseError,
                                @Param("retryCount") Integer retryCount);
}
