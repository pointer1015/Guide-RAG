package com.guiderag.knowledge.service.impl;


import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.guiderag.common.context.UserContextHolder;
import com.guiderag.common.exception.BizException;
import com.guiderag.common.config.MinioProperties;
import com.guiderag.knowledge.mapper.DocumentMapper;
import com.guiderag.knowledge.mapper.KnowledgeBaseMapper;
import com.guiderag.knowledge.model.dto.DocumentCreateReqDTO;
import com.guiderag.knowledge.model.dto.DocumentResDTO;
import com.guiderag.knowledge.model.dto.DocumentStatusResDTO;
import com.guiderag.knowledge.model.entity.Document;
import com.guiderag.knowledge.service.DocumentProcessService;
import com.guiderag.knowledge.service.DocumentService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "txt", "md");
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final DocumentProcessService documentProcessService;

    // 判断知识库是否存在
    @Override
    public Long createDocument(Long knowledgeBaseId, DocumentCreateReqDTO dto) {
        Long userId = UserContextHolder.getUserId();
        if (knowledgeBaseMapper.selectById(knowledgeBaseId, userId) == null) {
            throw new BizException("A0500", "知识库不存在或无权访问");
        }

        // 判断文档是否存在
        if (StringUtils.hasText(dto.getContentHash())) {
            Long count = documentMapper.countByKbAndHash(knowledgeBaseId, userId, dto.getContentHash());
            if (count != null && count > 0) {
                throw new BizException("A0400", "文档已存在，请勿重复上传");
            }
        }

        Document doc = new Document();
        doc.setId(IdUtil.getSnowflakeNextId());
        doc.setTenantId(userId);
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setUploadedBy(userId);
        doc.setTitle(dto.getTitle());
        doc.setFileName(dto.getFileName());
        doc.setFileType(dto.getFileType());
        doc.setFileSize(dto.getFileSize());
        doc.setMimeType(dto.getMimeType());
        doc.setMinioBucket(dto.getMinioBucket());
        doc.setMinioObjectKey(dto.getMinioObjectKey());
        doc.setContentHash(dto.getContentHash());
        doc.setParseStatus("PENDING");

        documentMapper.insert(doc);
        return doc.getId();
    }

    @Override
    public PageInfo<DocumentResDTO> list(Long knowledgeBaseId, int page, int size) {
        Long tenantId = currentTenantId();
        if (knowledgeBaseMapper.selectById(knowledgeBaseId, tenantId) == null) {
            throw new BizException("A0500", "知识库不存在或无权访问");
        }

        PageHelper.startPage(page, size);
        List<Document> list = documentMapper.selectByKnowledgeBaseId(knowledgeBaseId, tenantId);

        List<DocumentResDTO> dtoList = list.stream().map(this::toResDTO).collect(Collectors.toList());
        PageInfo<Document> pageInfo = new PageInfo<>(list);
        PageInfo<DocumentResDTO> result = new PageInfo<>();
        result.setList(dtoList);
        result.setTotal(pageInfo.getTotal());
        result.setPages(pageInfo.getPages());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        return result;
    }

    @Override
    public DocumentResDTO getById(Long knowledgeBaseId, Long documentId) {
        Document doc = documentMapper.selectById(documentId, knowledgeBaseId, currentTenantId());
        if (doc == null) {
            throw new BizException("A0500", "文档不存在或无权访问");
        }
        return toResDTO(doc);
    }

    @Override
    public DocumentStatusResDTO getStatus(Long knowledgeBaseId, Long documentId) {
        Document doc = documentMapper.selectById(documentId, knowledgeBaseId, currentTenantId());
        if (doc == null) {
            throw new BizException("A0500", "文档不存在或无权访问");
        }
        
        DocumentStatusResDTO statusDto = new DocumentStatusResDTO();
        statusDto.setDocId(doc.getId());
        statusDto.setStatus(doc.getParseStatus());
        statusDto.setErrorMessage(doc.getParseError());
        statusDto.setChunkCount(doc.getChunkCount());
        statusDto.setGmtModified(doc.getGmtModified());
        
        // 简单进度逻辑计算
        int progress = 0;
        if ("PARSED".equals(doc.getParseStatus())) {
            progress = 100;
        } else if ("PARSING".equals(doc.getParseStatus())) {
            progress = 50; // 流程中
        }
        statusDto.setProgress(progress);
        
        return statusDto;
    }

    @Override
    public void delete(Long knowledgeBaseId, Long documentId) {
        Long tenantId = currentTenantId();
        Document doc = documentMapper.selectById(documentId, knowledgeBaseId, tenantId);
        if (doc == null) {
            throw new BizException("A0500", "文档不存在或无权访问");
        }
        documentMapper.deleteById(documentId, knowledgeBaseId, tenantId);
    }

    @Override
    public Long upload(Long knowledgeBaseId, MultipartFile file, String title) {
        Long tenantId = currentTenantId();
        if (knowledgeBaseMapper.selectById(knowledgeBaseId, tenantId) == null) {
            throw new BizException("A0500", "知识库不存在或无权访问");
        }

        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BizException("A0401", "上传文件不能为空");
        }

        // 检查文件类型是否支持
        String originalFilename = file.getOriginalFilename();
        String ext = getExt(originalFilename);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("A0402", "不支持的文件类型");
        }

        try {
            // 检查文档是否已存在
            String hash = sha256(file);
            Long count = documentMapper.countByKbAndHash(knowledgeBaseId, tenantId, hash);
            if (count != null && count > 0) {
                throw new BizException("A0400", "文档已存在，请勿重复上传");
            }

            long docId = IdUtil.getSnowflakeNextId();
            String objectKey = "kb/" + knowledgeBaseId + "/" + docId + "_" + sanitizeFileName(originalFilename);

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            Document doc = new Document();
            doc.setId(docId);
            doc.setTenantId(tenantId);
            doc.setKnowledgeBaseId(knowledgeBaseId);
            doc.setUploadedBy(tenantId);
            doc.setTitle(title);
            doc.setFileName(originalFilename);
            doc.setFileType(ext);
            doc.setFileSize(file.getSize());
            doc.setMimeType(file.getContentType());
            doc.setMinioBucket(minioProperties.getBucketName());
            doc.setMinioObjectKey(objectKey);
            doc.setContentHash(hash);
            doc.setParseStatus("PENDING");

            documentMapper.insert(doc);

            // 触发异步解析处理
            documentProcessService.processDocumentAsync(tenantId, docId);

            return docId;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("B0001", "文档上传失败: " + e.getMessage());
        }
    }


    private Long currentTenantId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) throw new BizException("A0200", "未登录");
        return userId; // MVP: 用户即租户
    }

    private DocumentResDTO toResDTO(Document doc) {
        DocumentResDTO dto = new DocumentResDTO();
        dto.setId(doc.getId());
        dto.setKnowledgeBaseId(doc.getKnowledgeBaseId());
        dto.setTitle(doc.getTitle());
        dto.setFileName(doc.getFileName());
        dto.setFileType(doc.getFileType());
        dto.setFileSize(doc.getFileSize());
        dto.setMimeType(doc.getMimeType());
        dto.setParseStatus(doc.getParseStatus());
        dto.setGmtCreate(doc.getGmtCreate());
        dto.setGmtModified(doc.getGmtModified());
        return dto;
    }

    private String getExt(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) return "unknown";
        return fileName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String sha256(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = file.getInputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                digest.update(buf, 0, len);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
