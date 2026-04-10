package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.mapper.ApplicationMapper;
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
import java.util.HashMap;
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



    public IPage<ResourceUseVO> selectUserPage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType) {
        Page<ResourceMappingEntity> resourcePage = new Page<>(current, size);
        LambdaQueryWrapper<ResourceMappingEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ResourceMappingEntity::getTargetType, resourceType);
        wrapper.eq(ResourceMappingEntity::getTargetId, resourceId);
        if (StringUtils.isNotBlank(resourceName)) {
            wrapper.like(ResourceMappingEntity::getResourceName, resourceName);
        }
        if (sourceType != null && sourceType.length > 0) {
            wrapper.in(ResourceMappingEntity::getSourceType, Arrays.asList(sourceType));
        }
        // 提前获取用户ID列表，用于后续优化
        List<String> userIds;
        if (StringUtils.isNotBlank(userName)) {
            LambdaQueryWrapper<UserEntity> userWrapper = Wrappers.lambdaQuery();
            userWrapper.select(UserEntity::getId);
            userWrapper.like(UserEntity::getNickname, userName);
            userIds = userMapper.selectList(userWrapper).stream().map(UserEntity::getId).toList();
            if (CollectionUtils.isEmpty(userIds)) {
                return new Page<>(current, size);
            }
            wrapper.in(ResourceMappingEntity::getUserId, userIds);
        }
        wrapper.orderByDesc(ResourceMappingEntity::getCreateTime);

        resourcePage = this.page(resourcePage, wrapper);
        if (CollectionUtils.isEmpty(resourcePage.getRecords())) {
            return new Page<>(current, size);
        }

        // 批量查询用户昵称（仅查询涉及的用户）
        List<String> allUserIds = resourcePage.getRecords().stream()
                .map(ResourceMappingEntity::getUserId)
                .distinct()
                .toList();
        Map<String, String> nicknameMap = userMapper.selectByIds(allUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
        List<String> ids = resourcePage.getRecords().stream()
                .map(ResourceMappingEntity::getSourceId)
                .distinct()
                .toList();
        Map<String,Map<String, Object>> resourceMaps = new HashMap<>();
        List<ApplicationEntity> appList =applicationMapper.selectList(Wrappers.<ApplicationEntity>lambdaQuery()
                .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc, ApplicationEntity::getIcon, ApplicationEntity::getType)
                .in(ApplicationEntity::getId,ids));
        for (ApplicationEntity app : appList) {
            resourceMaps.put(app.getId(), BeanUtil.toMap(app));
        }
        List<KnowledgeEntity> knowledgeList =knowledgeMapper.selectList(Wrappers.<KnowledgeEntity>lambdaQuery()
                .select(KnowledgeEntity::getId, KnowledgeEntity::getName, KnowledgeEntity::getDesc, KnowledgeEntity::getType)
                .in(KnowledgeEntity::getId,ids));
        for (KnowledgeEntity Knowledge : knowledgeList) {
            resourceMaps.put(Knowledge.getId(), BeanUtil.toMap(Knowledge));
        }
        List<ToolEntity> toolList =toolMapper.selectList(Wrappers.<ToolEntity>lambdaQuery()
                .select(ToolEntity::getId, ToolEntity::getName, ToolEntity::getDesc, ToolEntity::getIcon, ToolEntity::getToolType)
                .in(ToolEntity::getId,ids));
        for (ToolEntity tool : toolList) {
            resourceMaps.put(tool.getId(), BeanUtil.toMap(tool));
        }
        return PageUtil.copy(resourcePage, resource -> {
            ResourceUseVO vo = BeanUtil.copy(resource, ResourceUseVO.class);
            String sourceId = resource.getSourceId();
            Map<String, Object> resourceMap = resourceMaps.get(sourceId);
            if (resourceMap != null) {
                vo.setName((String) resourceMap.get("name"));
                vo.setDesc((String) resourceMap.get("desc"));
                vo.setIcon((String) resourceMap.get("icon"));
                vo.setType((String) resourceMap.getOrDefault("type",resourceMap.get("toolType")));
            }
            vo.setUsername(nicknameMap.get(resource.getUserId()));
            return vo;
        });
    }


}
