package com.guiderag.chat.service.impl;

import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.guiderag.chat.mapper.SessionMapper;
import com.guiderag.chat.model.dto.SessionCreateReqDTO;
import com.guiderag.chat.model.dto.SessionResDTO;
import com.guiderag.chat.model.dto.SessionUpdateReqDTO;
import com.guiderag.chat.model.entity.Session;
import com.guiderag.chat.service.SessionService;
import com.guiderag.common.context.UserContextHolder;
import com.guiderag.common.exception.BizException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;

    @Override
    public SessionResDTO createSession(SessionCreateReqDTO dto) {
        // 获取当前登录的用户Id
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException("A0200", "未登录");
        }

        // 构建会话实体
        Session session = new Session();
        session.setId(IdUtil.getSnowflakeNextId()); //雪花ID
        session.setTenantId(userId); // 目前userId=tenantId
        session.setUserId(userId);
        session.setKnowledgeBaseId(dto.getKnowledgeBaseId()); // 关联的知识库
        session.setTitle(dto.getTitle() != null ? dto.getTitle() : "新会话"); // 会话标题
        session.setIsDeleted(0);

        // 插入数据库
        sessionMapper.insert(session);

        return toResDTO(session);
    }

    @Override
    public PageInfo<SessionResDTO> list(int page, int size) {
        // 获取当前登录的用户Id
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException("A0200", "未登录");
        }

        PageHelper.startPage(page, size);

        // 查询会话列表
        List<Session> sessions = sessionMapper.selectByTenantIdAndUserId(
            userId,
            userId
        );

        // 将普通的会话列表转换为DTO列表
        List<SessionResDTO> dtoList = sessions
            .stream()
            .map(this::toResDTO)
            .collect(Collectors.toList());

        // 构建分页结果
        PageInfo<Session> pageInfo = new PageInfo<>(sessions);
        PageInfo<SessionResDTO> resPageInfo = new PageInfo<>(dtoList);
        resPageInfo.setList(dtoList);
        resPageInfo.setTotal(pageInfo.getTotal());
        resPageInfo.setPageNum(pageInfo.getPageNum());
        resPageInfo.setPageSize(pageInfo.getPageSize());
        resPageInfo.setPages(pageInfo.getPages());

        return resPageInfo;
    }

    @Override
    public SessionResDTO getSessionById(Long sessionId) {
        // 获取当前登录的用户Id
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException("A0200", "未登录");
        }

        Session session = sessionMapper.selectById(sessionId, userId);
        if (session == null) {
            throw new BizException("A0500", "会话不存在");
        }

        return toResDTO(session);
    }

    @Override
    public void updateSession(Long sessionId, SessionUpdateReqDTO dto) {
        // 获取当前登录的用户Id
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException("A0200", "未登录");
        }

        // 校验会话是否属于当前用户
        Session session = sessionMapper.selectById(sessionId, userId);
        if (session == null) {
            throw new BizException("A0500", "会话不存在");
        }

        // 执行更新
        int rows = sessionMapper.updateTitle(sessionId, userId, dto.getTitle());
        if (rows == 0) {
            throw new BizException("B0001", "更新会话失败");
        }
    }

    @Override
    public void deleteSession(Long sessionId) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException("A0200", "未登录");
        }

        // 1. 校验会话是否存在且属于当前用户
        Session session = sessionMapper.selectById(sessionId, userId);
        if (session == null) {
            throw new BizException("A0500", "会话不存在或无权访问");
        }

        // 2. 执行删除
        int rows = sessionMapper.deleteById(sessionId, userId);
        if (rows == 0) {
            throw new BizException("B0001", "删除会话失败");
        }
    }

    /**
     * Entity → DTO 转换方法
     * <p>
     * 集中转换逻辑，便于后续维护和扩展
     * </p>
     */
    private SessionResDTO toResDTO(Session session) {
        SessionResDTO dto = new SessionResDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setKnowledgeBaseId(session.getKnowledgeBaseId());
        dto.setLastMessageAt(session.getLastMessageAt());
        dto.setGmtCreate(session.getGmtCreate());
        dto.setGmtModified(session.getGmtModified());
        return dto;
    }
}
