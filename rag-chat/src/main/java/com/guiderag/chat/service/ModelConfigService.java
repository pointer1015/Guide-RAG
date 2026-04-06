package com.guiderag.chat.service;

import com.guiderag.chat.model.dto.ModelConfigReqDTO;
import com.guiderag.chat.model.dto.ModelConfigResDTO;

/**
 * 用户模型配置服务
 */
public interface ModelConfigService {

    /**
     * 获取当前用户的模型配置
     * @return 配置信息（无配置时返回 null）
     */
    ModelConfigResDTO getMyConfig();

    /**
     * 保存（新增或更新）当前用户的模型配置
     * @param dto 配置请求
     * @return 保存后的配置信息
     */
    ModelConfigResDTO saveConfig(ModelConfigReqDTO dto);

    /**
     * 删除当前用户的模型配置（回退到系统默认模型）
     */
    void deleteConfig();
}
