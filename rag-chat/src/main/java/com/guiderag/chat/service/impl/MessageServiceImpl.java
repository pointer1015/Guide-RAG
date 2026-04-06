package com.guiderag.chat.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.guiderag.chat.mapper.MessageMapper;
import com.guiderag.chat.mapper.SessionMapper;
import com.guiderag.chat.model.dto.MessageCreateReqDTO;
import com.guiderag.chat.model.dto.MessageResDTO;
import com.guiderag.chat.model.entity.Message;
import com.guiderag.chat.model.entity.Session;
import com.guiderag.chat.service.MessageService;
import com.guiderag.common.context.UserContextHolder;
import com.guiderag.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final SessionMapper sessionMapper;

    @Override
    public Long createMessage(Long sessionId, MessageCreateReqDTO dto) {
        // 1. 获取当前用户ID
        Long userId = UserContextHolder.getUserId();

        // 2. 校验会话存在性
        Session session = sessionMapper.selectById(sessionId, userId);
        if (session == null) {
            throw new BizException("A0500", "会话不存在或无权访问");
        }

        // 构建消息实体
        Message message = new Message();
        message.setMessageId(IdUtil.getSnowflakeNextId());
        message.setTenantId(userId);
        message.setSessionId(sessionId);
        // user类型消息设置 userId，其余消息 userId 为空
        message.setUserId("user".equals(dto.getRole()) ? userId : null);
        message.setRole(dto.getRole());
        message.setContent(dto.getContent());
        message.setContentType(dto.getContentType() != null ? dto.getContentType() : "text");
        message.setReferencedChunkIds(dto.getReferencedChunkIds() != null ? dto.getReferencedChunkIds() : "[]");
        message.setRetrievalContext(dto.getRetrievalContext() != null ? dto.getRetrievalContext() : "{}");
        message.setTokenInput(dto.getTokenInput() != null ? dto.getTokenInput() : 0);
        message.setTokenOutput(dto.getTokenOutput() != null ? dto.getTokenOutput() : 0);
        message.setLatencyMs(dto.getLatencyMs() != null ? dto.getLatencyMs() : 0);
        message.setIsDeleted(0);

        messageMapper.insert(message);

        return message.getMessageId();
    }

    @Override
    public PageInfo<MessageResDTO> listBySessionId(Long sessionId, int page, int size, String role) {

        Long userId = UserContextHolder.getUserId();

        // 校验会话是否存在
        Session session = sessionMapper.selectById(sessionId, userId);
        if (session == null) {
            throw new BizException("A0500", "会话不存在或无权访问");
        }

        PageHelper.startPage(page, size);

        // 查询消息列表
        List<Message> messages = messageMapper.selectBySessionId(sessionId, userId, role);

        // 转换为DTO
        List<MessageResDTO> dtoList = messages.stream()
                .map(this::toResDTO)
                .collect(Collectors.toList());


        // 构建分页结果
        PageInfo<Message> pageInfo = new PageInfo<>(messages);
        PageInfo<MessageResDTO> result = new PageInfo<>();
        result.setList(dtoList);
        result.setTotal(pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        result.setPages(pageInfo.getPages());

        return result;
    }

     /**
     * 根据ID获取消息详情
     */
    @Override
    public MessageResDTO getById(Long messageId) {
        Long userId = UserContextHolder.getUserId();

        Message message = messageMapper.selectById(messageId, userId);
        if (message == null) {
            throw new BizException("A0500", "消息不存在或无权访问");
        }

        return toResDTO(message);
    }

    /**
     * 删除消息
     */
    @Override
    public void deleteMessage(Long messageId) {
        Long userId = UserContextHolder.getUserId();
        // 1. 校验消息存在性
        Message message = messageMapper.selectById(messageId, userId);
        if (message == null) {
            throw new BizException("A0500", "消息不存在或无权访问");
        }

        // 2. 执行软删除
        int rows = messageMapper.deleteById(messageId, userId);
        if (rows == 0) {
            throw new BizException("B0001", "删除消息失败");
        }
    }

    /**
     * 删除会话下所有消息
     */
    @Override
    public void deleteBySessionId(Long sessionId) {
        Long userId = UserContextHolder.getUserId();
        messageMapper.deleteBySessionId(sessionId, userId);
    }

    /**
     * Entity → DTO 转换
     * 解析 JSONB 字段中的引用信息
     */
    private MessageResDTO toResDTO(Message message) {
        MessageResDTO dto = new MessageResDTO();
        dto.setMessageId(message.getMessageId());
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setContentType(message.getContentType());
        // 创建时间
        dto.setGmtCreate(message.getGmtCreate());

        // 解析引用信息（仅 assistant 消息）
        if ("assistant".equals(message.getRole()) && StringUtils.hasText(message.getReferencedChunkIds())) {
            try {
                // retrievalContext 中可能存储更详细的引用信息
                String contextJson = message.getRetrievalContext();
                if (StringUtils.hasText(contextJson) && !"{}".equals(contextJson)) {
                    JSONObject context = JSON.parseObject(contextJson);
                    JSONArray citationsArray = context.getJSONArray("citations");
                    if (citationsArray != null && !citationsArray.isEmpty()) {
                        List<MessageResDTO.CitationDTO> citations = new ArrayList<>();
                        for (int i = 0; i < citationsArray.size(); i++) {
                            JSONObject citObj = citationsArray.getJSONObject(i);
                            MessageResDTO.CitationDTO citation = new MessageResDTO.CitationDTO();
                            citation.setDocId(citObj.getString("docId"));
                            citation.setChunkId(citObj.getString("chunkId"));
                            citation.setScore(citObj.getDouble("score"));
                            citation.setSourceText(citObj.getString("sourceText"));
                            citations.add(citation);
                        }
                        dto.setCitations(citations);
                    }
                }
            } catch (Exception e) {
                // 解析失败时忽略引用信息
                dto.setCitations(null);
            }
        }

        return dto;
    }
}
