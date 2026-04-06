package com.guiderag.chat.service;

import com.github.pagehelper.PageInfo;
import com.guiderag.chat.model.dto.MessageCreateReqDTO;
import com.guiderag.chat.model.dto.MessageResDTO;

public interface MessageService {

     /**
     * 创建消息
     *
     * @param sessionId 会话ID
     * @param dto       消息创建请求
     * @return 新消息ID
     */
    Long createMessage(Long sessionId, MessageCreateReqDTO dto);

    /**
     * 分页查询会话的历史消息
     *
     * @param sessionId 会话ID
     * @param page      页码
     * @param size      每页条数
     * @param role      消息角色过滤（可选）
     * @return 分页消息列表
     */
    PageInfo<MessageResDTO> listBySessionId(Long sessionId, int page, int size, String role);

    /**
     * 根据ID获取消息详情
     *
     * @param messageId 消息ID
     * @return 消息详情
     */
    MessageResDTO getById(Long messageId);

    /**
     * 删除消息
     *
     * @param messageId 消息ID
     */
    void deleteMessage(Long messageId);

    /**
     * 删除会话下所有消息（级联删除）
     *
     * @param sessionId 会话ID
     */
    void deleteBySessionId(Long sessionId);
}
