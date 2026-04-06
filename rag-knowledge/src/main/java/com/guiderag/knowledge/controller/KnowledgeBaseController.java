package com.guiderag.knowledge.controller;

import com.github.pagehelper.PageInfo;
import com.guiderag.common.result.Result;
import com.guiderag.knowledge.model.dto.KnowledgeBaseCreateReqDTO;
import com.guiderag.knowledge.model.dto.KnowledgeBaseResDTO;
import com.guiderag.knowledge.model.dto.KnowledgeBaseUpdateReqDTO;
import com.guiderag.knowledge.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "知识库管理")
@RestController
@RequestMapping("/rag/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @Operation(summary = "创建知识库")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody KnowledgeBaseCreateReqDTO dto) {
        Long result = knowledgeBaseService.create(dto);
        return Result.success(result);
    }

    @Operation(summary = "分页查询知识库列表")
    @GetMapping
    public Result<PageInfo<KnowledgeBaseResDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageInfo<KnowledgeBaseResDTO> result = knowledgeBaseService.list(page, size);
        return Result.success(result);
    }

    @Operation(summary = "获取知识库详情")
    @GetMapping("/{id}")
    public Result<KnowledgeBaseResDTO> getById(@PathVariable Long id) {
        KnowledgeBaseResDTO result = knowledgeBaseService.getById(id);
        return Result.success(result);
    }

    @Operation(summary = "更新知识库")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody KnowledgeBaseUpdateReqDTO dto) {
        knowledgeBaseService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return Result.success();
    }
}
