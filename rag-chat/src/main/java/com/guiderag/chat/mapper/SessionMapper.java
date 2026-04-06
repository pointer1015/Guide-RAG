package com.guiderag.chat.mapper;

import com.guiderag.chat.model.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface SessionMapper {
    /**
     * 插入新会话
     */
    int insert(Session session);

    /**
     * 根据 ID 和租户查询会话（排除已删除）
     */
    Session selectById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 分页查询当前用户的会话列表（排除已删除，按最后消息时间倒序）
     */
    List<Session> selectByTenantIdAndUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /**
     * 更新会话标题
     */
    int updateTitle(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("title") String title);

    /**
     * 删除会话
     */
    int deleteById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 更新会话最后消息时间
     *
     * @param sessionId 会话ID
     * @param userId 用户ID（租户隔离）
     * @param lastMessageAt 最后消息时间
     * @return 影响行数
     */
    int updateLastMessageAt(@Param("sessionId") Long sessionId,
                            @Param("userId") Long userId,
                            @Param("lastMessageAt") OffsetDateTime lastMessageAt);
}
