package com.guiderag.chat.service;

import com.github.pagehelper.PageInfo;
import com.guiderag.chat.model.dto.SessionCreateReqDTO;
import com.guiderag.chat.model.dto.SessionResDTO;
import com.guiderag.chat.model.dto.SessionUpdateReqDTO;

/**
 * 会话管理服务接口
 */
public interface SessionService {
    // 创建新会话，返回完整会话信息（让前端无需二次请求即可获得id和title）
    SessionResDTO createSession(SessionCreateReqDTO dto);

    // 分页查询当前用户的会话列表
    PageInfo<SessionResDTO> list(int page, int size);

    // 根据ID获取会话详情
    SessionResDTO getSessionById(Long sessionId);

    // 更新会话标题
    void updateSession(Long sessionId, SessionUpdateReqDTO dto);

    // 删除会话
    void deleteSession(Long sessionId);
}
