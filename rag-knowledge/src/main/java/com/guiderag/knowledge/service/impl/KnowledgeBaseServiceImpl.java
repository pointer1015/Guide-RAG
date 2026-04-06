package com.guiderag.knowledge.service.impl;

import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.guiderag.common.context.UserContextHolder;
import com.guiderag.common.exception.BizException;
import com.guiderag.knowledge.mapper.KnowledgeBaseMapper;
import com.guiderag.knowledge.model.dto.KnowledgeBaseCreateReqDTO;
import com.guiderag.knowledge.model.dto.KnowledgeBaseResDTO;
import com.guiderag.knowledge.model.dto.KnowledgeBaseUpdateReqDTO;
import com.guiderag.knowledge.model.entity.KnowledgeBase;
import com.guiderag.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.commons.util.IdUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public Long create(KnowledgeBaseCreateReqDTO dto) {
        // 1.获取当前用户登录Id
        Long userId = UserContextHolder.getUserId();

        // 2.校验知识库名称唯一性
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectByTenantIdAndName(userId, dto.getName());
        if (knowledgeBase != null) {
            throw new BizException("A0401", "知识库名称已存在，请换一个名称！");
        }

        // 3.构建实体并保存到数据库
        KnowledgeBase base = new KnowledgeBase();
        base.setId(IdUtil.getSnowflakeNextId());
        base.setTenantId(userId);
        base.setCreatedBy(userId);
        base.setName(dto.getName());
        base.setDescription(dto.getDescription());
        base.setVersion(0);

        knowledgeBaseMapper.insert(base);

        return base.getId();
    }

    @Override
    public PageInfo<KnowledgeBaseResDTO> list(int page, int size) {

        PageHelper.startPage(page, size);
        List<KnowledgeBase> kbList = knowledgeBaseMapper.selectByTenantId(UserContextHolder.getUserId());

        // 数据库查询的原始数据转换为ResDTO列表
        Long tenantId = UserContextHolder.getUserId();
        List<KnowledgeBaseResDTO> resDTOList = kbList.stream()
                .map(kb -> toResDTO(kb, tenantId))
                .collect(Collectors.toList());


        // 用原始 PageInfo 包装，替换列表为 DTO 列表
        PageInfo<KnowledgeBase> pageInfo = new PageInfo<>(kbList);
        PageInfo<KnowledgeBaseResDTO> result = new PageInfo<>();
        result.setList(resDTOList);
        result.setTotal(pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        result.setPages(pageInfo.getPages());
        return result;
    }

    @Override
    public KnowledgeBaseResDTO getById(Long id) {

        Long userId = UserContextHolder.getUserId();

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id, userId);
        if (knowledgeBase == null) {
            throw new BizException("A0402", "知识库不存在或无访问权限！");
        }

        return toResDTO(knowledgeBase, userId);
    }

    @Override
    public void update(Long id, KnowledgeBaseUpdateReqDTO dto) {

        Long userId = UserContextHolder.getUserId();

        // 1. 先确认知识库存在且属于当前用户
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id, userId);
        if (kb == null) {
            throw new BizException("A0500", "知识库不存在或无权访问");
        }

        // 2. 如果要改名，校验新名称不与其他知识库冲突
        if (dto.getName() != null && !dto.getName().equals(kb.getName())) {
            KnowledgeBase nameConflict = knowledgeBaseMapper.selectByTenantIdAndName(userId, dto.getName());
            if (nameConflict != null) {
                throw new BizException("A0410", "知识库名称已存在，请换一个名称");
            }
        }

        // 3. 构建更新实体
        KnowledgeBase updateEntity = new KnowledgeBase();
        updateEntity.setId(id);
        updateEntity.setTenantId(userId);
        updateEntity.setName(dto.getName());
        updateEntity.setDescription(dto.getDescription());
        knowledgeBaseMapper.updateById(updateEntity);
    }

    @Override
    public void delete(Long id) {

        Long userId = UserContextHolder.getUserId();

        // 1. 先确认知识库存在且属于当前用户
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id, userId);
        if (kb == null) {
            throw new BizException("A0500", "知识库不存在或无权访问");
        }

        // 2. TODO：待 Document 模块完成后，在此处补充关联文档检查
        //    逻辑：查询该知识库下 is_deleted=0 的文档数量，若 > 0 则拒绝删除
        long docCount = knowledgeBaseMapper.countActiveDocumentByKnowledgeBaseId(id, userId);
        if (docCount > 0) {
            throw new BizException("A0411", "知识库非空，不可删除，请先删除文档");
        }

        // 3. 执行软删除
        knowledgeBaseMapper.deleteById(id, userId);
    }

    /**
     * 数据库查询的原始数据 → ResDTO 转换
     * 集中在一处转换，方便后续维护
     */
    private KnowledgeBaseResDTO toResDTO(KnowledgeBase kb, Long tenantId) {
        KnowledgeBaseResDTO dto = new KnowledgeBaseResDTO();
        dto.setId(kb.getId());
        dto.setName(kb.getName());
        dto.setDescription(kb.getDescription());
        dto.setCreatedBy(kb.getCreatedBy());
        dto.setGmtCreate(kb.getGmtCreate());
        dto.setGmtModified(kb.getGmtModified());
        
        long count = knowledgeBaseMapper.countActiveDocumentByKnowledgeBaseId(kb.getId(), tenantId);
        dto.setDocCount((int) count);
        
        return dto;
    }
}
