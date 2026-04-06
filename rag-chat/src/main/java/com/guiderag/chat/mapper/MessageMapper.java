package com.guiderag.chat.mapper;

import com.guiderag.chat.model.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    /**
     * 插入消息
     *
     * @param message 消息实体
     * @return 影响行数
     */
    int insert(Message message);

    /**
     * 根据会话ID分页查询消息列表
     *
     * @param sessionId 会话ID
     * @param tenantId  租户ID
     * @param role      消息角色（可选，若不为空则过滤特定角色的消息）
     * @return 消息列表
     */
    List<Message> selectBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("tenantId") Long tenantId,
            @Param("role") String role
    );

    /**
     * 根据ID查询消息
     *
     * @param id       消息ID
     * @param tenantId 租户ID
     * @return 消息实体
     */
    Message selectById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 软删除消息
     *
     * @param id       消息ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 批量软删除会话下所有消息（用于删除会话时级联清理）
     *
     * @param sessionId 会话ID
     * @param tenantId  租户ID
     * @return 影响行数
     */
    int deleteBySessionId(@Param("sessionId") Long sessionId, @Param("tenantId") Long tenantId);

    /**
     * 【新增】查询最近的历史消息（用于多轮对话上下文）
     * 
     * 用途：为 LLM 提供会话历史上下文，支持多轮对话
     * 
     * @param sessionId 会话ID
     * @param tenantId  租户ID
     * @param limit     最大条数（建议10-20条）
     * @return 消息列表（按时间倒序，需在业务层反转）
     */
    List<Message> selectRecentBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("tenantId") Long tenantId,
            @Param("limit") int limit
    );
}
