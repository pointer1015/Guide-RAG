package com.guiderag.chat.controller;

import com.github.pagehelper.PageInfo;
import com.guiderag.chat.model.dto.MessageCreateReqDTO;
import com.guiderag.chat.model.dto.MessageResDTO;
import com.guiderag.chat.service.MessageService;
import com.guiderag.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息管理", description = "会话中的消息查询与管理")
@RestController
@RequestMapping("/rag/v1/sessions/{sessionId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 获取会话历史消息（分页）
     *
     * @param sessionId 会话ID
     * @param page      页码
     * @param size      每页条数
     * @param role      消息角色过滤（可选）
     * @return 分页消息列表
     */
    @Operation(summary = "获取会话历史消息", description = "分页查询指定会话的历史消息，支持按角色过滤")
    @GetMapping
    public Result<PageInfo<MessageResDTO>> list(
            @Parameter(description = "会话ID", required = true) @PathVariable Long sessionId,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "50") int size,
            @Parameter(description = "消息角色过滤：user/assistant/system") @RequestParam(required = false) String role
    ) {
        PageInfo<MessageResDTO> pageInfo = messageService.listBySessionId(sessionId, page, size, role);
        return Result.success(pageInfo);
    }

    /**
     * 创建消息（内部使用，Chat 接口会调用）
     *
     * @param sessionId 会话ID
     * @param dto       消息创建请求
     * @return 新消息ID
     */
    @Operation(summary = "创建消息", description = "在指定会话中创建一条消息（通常由 Chat 接口内部调用）")
    @PostMapping
    public Result<Long> create(
            @Parameter(description = "会话ID", required = true) @PathVariable Long sessionId,
            @Valid @RequestBody MessageCreateReqDTO dto
    ) {
        Long messageId = messageService.createMessage(sessionId, dto);
        return Result.success(messageId);
    }

    /**
     * 获取消息详情
     *
     * @param sessionId 会话ID
     * @param messageId 消息ID
     * @return 消息详情
     */
    @Operation(summary = "获取消息详情", description = "根据消息ID获取详细信息")
    @GetMapping("/{messageId}")
    public Result<MessageResDTO> getById(
            @Parameter(description = "会话ID", required = true) @PathVariable Long sessionId,
            @Parameter(description = "消息ID", required = true) @PathVariable Long messageId
    ) {
        MessageResDTO message = messageService.getById(messageId);
        return Result.success(message);
    }

    /**
     * 删除消息
     *
     * @param sessionId 会话ID
     * @param messageId 消息ID
     * @return 操作结果
     */
    @Operation(summary = "删除消息", description = "软删除指定消息")
    @DeleteMapping("/{messageId}")
    public Result<Void> delete(
            @Parameter(description = "会话ID", required = true) @PathVariable Long sessionId,
            @Parameter(description = "消息ID", required = true) @PathVariable Long messageId
    ) {
        messageService.deleteMessage(messageId);
        return Result.success();
    }
}
