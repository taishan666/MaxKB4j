package com.maxkb4j.system.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.constant.Permission;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.entity.SystemSettingEntity;
import com.maxkb4j.system.mapper.ResourceMappingMapper;
import com.maxkb4j.system.mapper.SystemSettingMapper;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.mapper.ToolMapper;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.entity.UserResourcePermissionEntity;
import com.maxkb4j.user.mapper.UserMapper;
import com.maxkb4j.user.vo.ResourceUseVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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



    public IPage<ResourceUseVO> selectUserPage(String resourceId, int current, int size, String resourceName, String userName, String[] sourceType, String model) {
        Page<ResourceMappingEntity> appPage = new Page<>(current, size);
        LambdaQueryWrapper<ResourceMappingEntity> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.isNotBlank(model)) {
            wrapper.eq(ResourceMappingEntity::getTargetType, model);
        }
        if (StringUtils.isNotBlank(resourceId)) {
            wrapper.eq(ResourceMappingEntity::getTargetId, resourceId);
        }
        if (StringUtils.isNotBlank(resourceName)) {
            wrapper.like(ResourceMappingEntity::getResourceName, resourceName);
        }
        if (sourceType != null && sourceType.length > 0) {
            wrapper.in(ResourceMappingEntity::getSourceType, sourceType);
        }
        if (StringUtils.isNotBlank(userName)) {
            LambdaQueryWrapper<UserEntity> userWwrapper = Wrappers.lambdaQuery();
            userWwrapper.like(UserEntity::getUsername, userName);
            wrapper.in(ResourceMappingEntity::getUserId, userMapper.selectList(userWwrapper).stream().map(UserEntity::getId).toList());
        }
        wrapper.orderByDesc(ResourceMappingEntity::getCreateTime);
        Map<String, String> nicknameMap = userMapper.selectList(new LambdaQueryWrapper<>()).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
        appPage = this.page(appPage, wrapper);
        return PageUtil.copy(appPage, app -> {
            ResourceUseVO vo = BeanUtil.copy(app, ResourceUseVO.class);
            if (ResourceType.APPLICATION.equals(app.getSourceType())) {
                ApplicationEntity application = applicationMapper.selectById(app.getSourceId());
                if (application != null) {
                    vo.setName(application.getName());
                    vo.setDesc(application.getDesc());
                }
            } else if (ResourceType.KNOWLEDGE.equals(app.getSourceType())) {
                KnowledgeEntity knowledge = knowledgeMapper.selectById(app.getSourceId());
                vo.setName(knowledge.getName());
                vo.setDesc(knowledge.getDesc());
            } else if (ResourceType.TOOL.equals(app.getSourceType())) {
                ToolEntity tool = toolMapper.selectById(app.getSourceId());
                vo.setName(tool.getName());
                vo.setDesc(tool.getDesc());
            }
            vo.setUsername(nicknameMap.get(app.getUserId()));
            return vo;
        });
    }
}
