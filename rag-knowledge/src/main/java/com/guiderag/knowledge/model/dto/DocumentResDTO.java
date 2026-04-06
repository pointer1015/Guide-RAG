package com.guiderag.knowledge.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentResDTO {
    private Long id;
    private Long knowledgeBaseId;
    private String title;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String mimeType;
    private String parseStatus;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
