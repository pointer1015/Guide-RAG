package com.guiderag.knowledge.mapper;

import com.guiderag.knowledge.model.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper {

    // 插入一条新的知识库记录，返回受影响的行数（通常为 1）
    int insert(KnowledgeBase record);

    // 根据Id查询
    KnowledgeBase selectById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    // 查询某租户下的所有知识库（配合 PageHelper 分页）
    List<KnowledgeBase> selectByTenantId(@Param("tenantId") Long tenantId);

    // 查询某租户下指定名称的知识库（用于重名校验）
    KnowledgeBase selectByTenantIdAndName(@Param("tenantId") Long tenantId, @Param("name") String name);

    // 更新知识库信息
    int updateById(KnowledgeBase kb);

    // 删除知识库
    int deleteById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    //
    long countActiveDocumentByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId,
                                              @Param("tenantId") Long tenantId);
}
