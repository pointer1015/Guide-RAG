package com.guiderag.chat.service.impl;

import com.guiderag.chat.mapper.UserModelConfigMapper;
import com.guiderag.chat.model.dto.ModelConfigReqDTO;
import com.guiderag.chat.model.dto.ModelConfigResDTO;
import com.guiderag.chat.model.entity.UserModelConfig;
import com.guiderag.chat.service.ModelConfigService;
import com.guiderag.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户模型配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final UserModelConfigMapper userModelConfigMapper;

    @Override
    public ModelConfigResDTO getMyConfig() {
        Long userId = UserContextHolder.getUserId();
        UserModelConfig config = userModelConfigMapper.selectByTenantAndUser(userId, userId);
        if (config == null) {
            return null;
        }
        return toResDTO(config);
    }

    @Override
    public ModelConfigResDTO saveConfig(ModelConfigReqDTO dto) {
        Long userId = UserContextHolder.getUserId();

        // 查询是否已有配置
        UserModelConfig existing = userModelConfigMapper.selectByTenantAndUser(userId, userId);

        if (existing != null) {
            // 更新已有配置
            existing.setProvider(dto.getProvider());
            existing.setApiKey(dto.getApiKey());
            existing.setBaseUrl(dto.getBaseUrl());
            existing.setModel(dto.getModel());
            existing.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : 1);
            userModelConfigMapper.updateByTenantAndUser(existing);
            log.info("更新用户模型配置: userId={}, provider={}, model={}", userId, dto.getProvider(), dto.getModel());
            // 重新查询获取更新后的时间戳
            existing = userModelConfigMapper.selectByTenantAndUser(userId, userId);
            return toResDTO(existing);
        } else {
            // 新增配置
            UserModelConfig config = new UserModelConfig();
            config.setTenantId(userId);
            config.setUserId(userId);
            config.setProvider(dto.getProvider());
            config.setApiKey(dto.getApiKey());
            config.setBaseUrl(dto.getBaseUrl());
            config.setModel(dto.getModel());
            config.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : 1);
            userModelConfigMapper.insert(config);
            log.info("新增用户模型配置: userId={}, provider={}, model={}", userId, dto.getProvider(), dto.getModel());
            // 重新查询获取完整数据
            config = userModelConfigMapper.selectByTenantAndUser(userId, userId);
            return toResDTO(config);
        }
    }

    @Override
    public void deleteConfig() {
        Long userId = UserContextHolder.getUserId();
        userModelConfigMapper.deleteByTenantAndUser(userId, userId);
        log.info("删除用户模型配置，回退默认模型: userId={}", userId);
    }

    /**
     * 实体转响应 DTO
     * API Key 脱敏处理：只显示前4位和后4位
     */
    private ModelConfigResDTO toResDTO(UserModelConfig entity) {
        ModelConfigResDTO dto = new ModelConfigResDTO();
        dto.setId(entity.getId());
        dto.setProvider(entity.getProvider());
        dto.setApiKey(maskApiKey(entity.getApiKey()));
        dto.setBaseUrl(entity.getBaseUrl());
        dto.setModel(entity.getModel());
        dto.setIsActive(entity.getIsActive());
        dto.setGmtCreate(entity.getGmtCreate());
        dto.setGmtModified(entity.getGmtModified());
        return dto;
    }

    /**
     * API Key 脱敏
     * sk-abcdef1234567890 → sk-abcd****7890
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return apiKey;
        }
        int visiblePrefix = 4;
        int visibleSuffix = 4;
        return apiKey.substring(0, visiblePrefix)
                + "****"
                + apiKey.substring(apiKey.length() - visibleSuffix);
    }
}
