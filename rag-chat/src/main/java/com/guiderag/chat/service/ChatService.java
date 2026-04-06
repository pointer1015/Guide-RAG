package com.guiderag.chat.service;

import com.guiderag.chat.model.dto.ChatReqDTO;
import com.guiderag.chat.model.dto.ChatResDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    /**
     * 同步聊天接口
     *
     * @param sessionId 会话ID
     * @param dto 聊天请求
     * @return 聊天响应（包含回答和引用）
     */
    ChatResDTO chat(Long sessionId, ChatReqDTO dto);

     /**
     * 流式聊天接口（SSE）
     *
     * 通过 Server-Sent Events 实时推送 LLM 生成的每个 Token，
     * 实现类似 ChatGPT 的打字机效果。
     *
     * @param sessionId 会话ID
     * @param dto 聊天请求
     * @return SseEmitter 用于推送流式数据
     */
    SseEmitter chatStream(Long sessionId, ChatReqDTO dto);
}


