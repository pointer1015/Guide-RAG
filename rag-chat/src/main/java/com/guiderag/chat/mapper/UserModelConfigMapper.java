package com.guiderag.chat.mapper;

import com.guiderag.chat.model.entity.UserModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户模型配置 Mapper
 */
@Mapper
public interface UserModelConfigMapper {

    /**
     * 根据租户ID和用户ID查询模型配置
     */
    UserModelConfig selectByTenantAndUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /**
     * 插入新配置
     */
    int insert(UserModelConfig config);

    /**
     * 更新配置（根据 tenant_id + user_id）
     */
    int updateByTenantAndUser(UserModelConfig config);

    /**
     * 删除配置（物理删除）
     */
    int deleteByTenantAndUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
