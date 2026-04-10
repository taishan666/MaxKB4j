package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.mapper.ResourceMappingMapper;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.mapper.ToolMapper;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.mapper.UserMapper;
import com.maxkb4j.user.vo.ResourceUseVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 小峰
 * @date 2026-04-08 17:33:32
 */
@Service
@RequiredArgsConstructor
public class ResourceMappingService extends ServiceImpl<ResourceMappingMapper, ResourceMappingEntity> {
    private final UserMapper userMapper;
    private final ApplicationMapper applicationMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final ToolMapper toolMapper;



    public IPage<ResourceUseVO> selectUserPage(String resourceType,String resourceId, int current, int size, String resourceName, String userName, String[] sourceType) {
        Page<ResourceMappingEntity> resourcePage = new Page<>(current, size);
        LambdaQueryWrapper<ResourceMappingEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ResourceMappingEntity::getTargetType, resourceType);
        wrapper.eq(ResourceMappingEntity::getTargetId, resourceId);
        if (StringUtils.isNotBlank(resourceName)) {
            wrapper.like(ResourceMappingEntity::getResourceName, resourceName);
        }
        if (sourceType!=null && sourceType.length > 0) {
            wrapper.in(ResourceMappingEntity::getSourceType, Arrays.asList(sourceType));
        }
        if (StringUtils.isNotBlank(userName)) {
            LambdaQueryWrapper<UserEntity> userWrapper = Wrappers.lambdaQuery();
            userWrapper.select(UserEntity::getId);
            userWrapper.like(UserEntity::getNickname, userName);
            List<String> userIds = userMapper.selectList(userWrapper).stream().map(UserEntity::getId).toList();
            if (CollectionUtils.isNotEmpty(userIds)){
                wrapper.in(ResourceMappingEntity::getUserId, userIds);
            }else {
                wrapper.last(" limit 0");
            }
        }
        wrapper.orderByDesc(ResourceMappingEntity::getCreateTime);
        Map<String, String> nicknameMap = userMapper.selectList(new LambdaQueryWrapper<>()).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
        resourcePage = this.page(resourcePage, wrapper);
        return PageUtil.copy(resourcePage, resource -> {
            ResourceUseVO vo = BeanUtil.copy(resource, ResourceUseVO.class);
            if (ResourceType.APPLICATION.equals(resource.getSourceType())) {
                ApplicationEntity application = applicationMapper.selectById(resource.getSourceId());
                if (application != null) {
                    vo.setName(application.getName());
                    vo.setDesc(application.getDesc());
                }
            } else if (ResourceType.KNOWLEDGE.equals(resource.getSourceType())) {
                KnowledgeEntity knowledge = knowledgeMapper.selectById(resource.getSourceId());
                vo.setName(knowledge.getName());
                vo.setDesc(knowledge.getDesc());
            } else if (ResourceType.TOOL.equals(resource.getSourceType())) {
                ToolEntity tool = toolMapper.selectById(resource.getSourceId());
                vo.setName(tool.getName());
                vo.setDesc(tool.getDesc());
            }
            vo.setUsername(nicknameMap.get(resource.getUserId()));
            return vo;
        });
    }
}
