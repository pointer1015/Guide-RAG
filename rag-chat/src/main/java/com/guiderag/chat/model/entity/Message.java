package com.guiderag.chat.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {

    // 消息Id
    private Long messageId;

    // 归属租户Id
    private Long tenantId;

    // 会话Id
    private Long sessionId;

    // 发送者Id
    private Long userId;

    // 消息角色
    private String role;

    // 消息正文内容
    private String content;

    // 消息内容类型
    private String contentType;

    // AI回复时引用的知识块Id集合
    private String referencedChunkIds;

    // 检索命中的上下文快照
    private String retrievalContext;

    // 本次提问消耗的token
    private Integer tokenInput;

    // 本次回答消耗的token
    private Integer tokenOutput;

    // 大模型响应或流式首字耗时
    private Integer latencyMs;

    // 消息创建的时间
    private LocalDateTime gmtCreate;

    // 逻辑删除 0-正常， 1-已删除
    private Integer isDeleted;
}


