package com.guiderag.knowledge.service;

import com.github.pagehelper.PageInfo;
import com.guiderag.knowledge.model.dto.KnowledgeBaseCreateReqDTO;
import com.guiderag.knowledge.model.dto.KnowledgeBaseResDTO;
import com.guiderag.knowledge.model.dto.KnowledgeBaseUpdateReqDTO;

public interface KnowledgeBaseService {

    // 创建知识库，返回新知识库的 ID
    Long create(KnowledgeBaseCreateReqDTO dto);

    // 分页查询当前用户的知识库列表
    PageInfo<KnowledgeBaseResDTO> list(int page, int size);

    // 根据 ID 获取知识库详情（当前用户只能查自己的）
    KnowledgeBaseResDTO getById(Long id);

    // 更新知识库信息
    void update(Long id, KnowledgeBaseUpdateReqDTO dto);

    // 删除知识库（软删除）
    void delete(Long id);
}
