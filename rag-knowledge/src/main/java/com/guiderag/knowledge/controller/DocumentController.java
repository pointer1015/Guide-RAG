package com.guiderag.knowledge.controller;

import com.github.pagehelper.PageInfo;
import com.guiderag.common.result.Result;
import com.guiderag.knowledge.model.dto.DocumentResDTO;
import com.guiderag.knowledge.model.dto.DocumentStatusResDTO;
import com.guiderag.knowledge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文档管理")
@RestController
@RequestMapping("/rag/v1/knowledge-bases/{knowledgeBaseId}/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    /**
     * 上传文档（主入口）
     * 前端 uploadDocument() 将 multipart/form-data 发送到 POST /documents，
     * 此接口接收文件并完成 MinIO 上传 + 解析触发流程。
     */
    @Operation(summary = "上传文档")
    @PostMapping(consumes = "multipart/form-data")
    public Result<Long> uploadDocument(
            @PathVariable Long knowledgeBaseId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title
    ) {
        return Result.success(documentService.upload(knowledgeBaseId, file, title));
    }

    /**
     * 上传文档（兼容路径）
     * 保留 /upload 子路径，与旧调用方保持兼容。
     */
    @Operation(summary = "上传文档（兼容路径）")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Result<Long> upload(
            @PathVariable Long knowledgeBaseId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title
    ) {
        return Result.success(documentService.upload(knowledgeBaseId, file, title));
    }

    @Operation(summary = "分页查询文档")
    @GetMapping
    public Result<PageInfo<DocumentResDTO>> list(
            @PathVariable Long knowledgeBaseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageInfo<DocumentResDTO> pageInfo = documentService.list(knowledgeBaseId, page, size);
        return Result.success(pageInfo);
    }

    @Operation(summary = "文档详情")
    @GetMapping("/{documentId}")
    public Result<DocumentResDTO> getById(
            @PathVariable Long knowledgeBaseId,
            @PathVariable Long documentId) {
        return Result.success(documentService.getById(knowledgeBaseId, documentId));
    }

    @Operation(summary = "文档状态")
    @GetMapping("/{documentId}/status")
    public Result<DocumentStatusResDTO> getStatus(
            @PathVariable Long knowledgeBaseId,
            @PathVariable Long documentId) {
        return Result.success(documentService.getStatus(knowledgeBaseId, documentId));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{documentId}")
    public Result<Void> delete(
            @PathVariable Long knowledgeBaseId,
            @PathVariable Long documentId) {
        documentService.delete(knowledgeBaseId, documentId);
        return Result.success();
    }
}
