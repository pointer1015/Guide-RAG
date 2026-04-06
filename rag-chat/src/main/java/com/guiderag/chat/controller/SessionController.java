package com.guiderag.chat.controller;

import com.github.pagehelper.PageInfo;
import com.guiderag.chat.model.dto.ChatReqDTO;
import com.guiderag.chat.model.dto.ChatResDTO;
import com.guiderag.chat.model.dto.SessionCreateReqDTO;
import com.guiderag.chat.model.dto.SessionResDTO;
import com.guiderag.chat.model.dto.SessionUpdateReqDTO;
import com.guiderag.chat.service.ChatService;
import com.guiderag.chat.service.SessionService;
import com.guiderag.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "会话管理", description = "会话的创建、查询、更新、删除及对话")
@RestController
@RequestMapping("/rag/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final ChatService chatService;

    // 创建会话
    @Operation(
        summary = "创建会话",
        description = "创建一个新的对话会话，可选关联知识库"
    )
    @PostMapping
    public Result<SessionResDTO> create(
        @Valid @RequestBody SessionCreateReqDTO dto
    ) {
        SessionResDTO session = sessionService.createSession(dto);
        return Result.success(session);
    }

    // 分页查询会话列表
    @Operation(
        summary = "会话列表",
        description = "分页查询当前用户的会话列表，按最后消息时间倒序"
    )
    @GetMapping
    public Result<PageInfo<SessionResDTO>> list(
        @Parameter(description = "页码，从1开始") @RequestParam(
            defaultValue = "1"
        ) int page,
        @Parameter(description = "每页条数") @RequestParam(
            defaultValue = "10"
        ) int size
    ) {
        PageInfo<SessionResDTO> pageInfo = sessionService.list(page, size);
        return Result.success(pageInfo);
    }

    // 获取会话详情
    @Operation(summary = "会话详情", description = "根据会话ID获取会话详情")
    @GetMapping("/{sessionId}")
    public Result<SessionResDTO> getById(
        @PathVariable("sessionId") Long sessionId
    ) {
        SessionResDTO session = sessionService.getSessionById(sessionId);
        return Result.success(session);
    }

    // 更新会话详情
    @Operation(summary = "更新会话", description = "更新会话标题")
    @PutMapping("/{sessionId}")
    public Result<Void> update(
        @PathVariable("sessionId") Long sessionId,
        @RequestBody SessionUpdateReqDTO dto
    ) {
        sessionService.updateSession(sessionId, dto);
        return Result.success();
    }

    // 删除会话
    @Operation(summary = "删除会话", description = "删除会话（逻辑删除）")
    @DeleteMapping("/{sessionId}")
    public Result<Void> delete(@PathVariable("sessionId") Long sessionId) {
        sessionService.deleteSession(sessionId);
        return Result.success();
    }

    /**
     * 发送消息并获取 AI 回答（同步版本）
     *
     * @param sessionId 会话ID
     * @param dto 聊天请求
     * @return AI 回答及引用信息
     */
    @Operation(
        summary = "发送消息（同步）",
        description = "向指定会话发送消息，同步返回 AI 回答。包含知识库检索和引用来源。"
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "500", description = "服务器错误"),
        }
    )
    @PostMapping("/{sessionId}/chat")
    public Result<ChatResDTO> chat(
        @Parameter(
            description = "会话ID",
            required = true
        ) @PathVariable Long sessionId,
        @Valid @RequestBody ChatReqDTO dto
    ) {
        ChatResDTO response = chatService.chat(sessionId, dto);
        return Result.success(response);
    }

    /**
     * 发送消息并获取 AI 流式回答（SSE）
     *
     * SSE 事件协议：
     * - event: citations → 检索到的引用来源（首先推送）
     * - event: token → 逐字推送的回答内容
     * - event: metaData → 完成时的元数据（messageId, tokenInput, tokenOutput, done=true）
     * - event: error → 错误信息
     *
     * @param sessionId 会话ID
     * @param dto 聊天请求
     * @return SSE 事件流
     */
    @Operation(
        summary = "发送消息（流式）",
        description = "向指定会话发送消息，通过 SSE 流式返回 AI 回答。支持打字机效果。"
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "SSE 事件流"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "500", description = "服务器错误"),
        }
    )
    @PostMapping(
        value = "/{sessionId}/chat/stream",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter chatStream(
        @Parameter(
            description = "会话ID",
            required = true
        ) @PathVariable Long sessionId,
        @Valid @RequestBody ChatReqDTO dto
    ) {
        return chatService.chatStream(sessionId, dto);
    }
}
