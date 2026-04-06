package com.guiderag.chat.controller;

import com.guiderag.chat.model.dto.ModelConfigReqDTO;
import com.guiderag.chat.model.dto.ModelConfigResDTO;
import com.guiderag.chat.service.ModelConfigService;
import com.guiderag.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户模型配置 API
 * 每个用户可以保存一份自己的自定义模型配置
 */
@Tag(name = "模型配置", description = "用户自定义 LLM 模型配置管理")
@RestController
@RequestMapping("/rag/v1/model-config")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    /**
     * 获取当前用户的模型配置
     */
    @Operation(summary = "获取模型配置", description = "获取当前用户保存的自定义模型配置，未配置时返回 null")
    @GetMapping
    public Result<ModelConfigResDTO> getMyConfig() {
        ModelConfigResDTO config = modelConfigService.getMyConfig();
        return Result.success(config);
    }

    /**
     * 保存（新增或更新）模型配置
     */
    @Operation(summary = "保存模型配置", description = "保存或更新当前用户的自定义模型配置")
    @PostMapping
    public Result<ModelConfigResDTO> saveConfig(@Valid @RequestBody ModelConfigReqDTO dto) {
        ModelConfigResDTO config = modelConfigService.saveConfig(dto);
        return Result.success(config);
    }

    /**
     * 删除模型配置（回退到系统默认模型）
     */
    @Operation(summary = "删除模型配置", description = "删除自定义模型配置，回退使用系统默认模型")
    @DeleteMapping
    public Result<Void> deleteConfig() {
        modelConfigService.deleteConfig();
        return Result.success();
    }
}
