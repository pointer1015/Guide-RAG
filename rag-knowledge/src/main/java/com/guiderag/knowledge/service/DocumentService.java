package com.guiderag.knowledge.service;

import com.github.pagehelper.PageInfo;
import com.guiderag.knowledge.model.dto.DocumentCreateReqDTO;
import com.guiderag.knowledge.model.dto.DocumentResDTO;
import com.guiderag.knowledge.model.dto.DocumentStatusResDTO;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    // 创建文档
    Long createDocument(Long knowledgeBaseId, DocumentCreateReqDTO dto);

    PageInfo<DocumentResDTO> list(Long knowledgeBaseId, int page, int size);

    DocumentResDTO getById(Long knowledgeBaseId, Long documentId);

    void delete(Long knowledgeBaseId, Long documentId);

    Long upload(Long knowledgeBaseId, MultipartFile file, String title);

    DocumentStatusResDTO getStatus(Long knowledgeBaseId, Long documentId);
}
