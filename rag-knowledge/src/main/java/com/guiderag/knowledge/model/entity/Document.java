package com.guiderag.knowledge.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Document {

    private Long id;
    private Long tenantId;
    private Long knowledgeBaseId;
    private Long uploadedBy;
    private String title;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String mimeType;
    private String minioBucket;
    private String minioObjectKey;
    private String contentHash;
    private String parseStatus;
    private String parseError;
    private Integer retryCount;
    private Integer chunkCount;
    private LocalDateTime parsedAt;
    private Integer version;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    private Integer isDeleted;
}
