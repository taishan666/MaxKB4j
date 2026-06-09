package com.maxkb4j.tool.service.resolver;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.system.entity.SourceResource;
import com.maxkb4j.system.strategy.SourceResourceResolver;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.mapper.ToolMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ToolSourceResolver implements SourceResourceResolver {

    private final ToolMapper toolMapper;

    @Override
    public String resourceType() {
        return ResourceType.TOOL;
    }

    @Override
    public List<SourceResource> resolve(Collection<String> ids, String resourceName, List<String> userIdFilter) {
        List<ToolEntity> tools = toolMapper.selectList(Wrappers.<ToolEntity>lambdaQuery()
                .select(ToolEntity::getId, ToolEntity::getName, ToolEntity::getDesc, ToolEntity::getIcon, ToolEntity::getToolType, ToolEntity::getUserId)
                .like(StringUtils.isNotBlank(resourceName), ToolEntity::getName, resourceName)
                .in(userIdFilter != null, ToolEntity::getUserId, userIdFilter)
                .in(ToolEntity::getId, ids));
        return tools.stream()
                .map(e -> new SourceResource(e.getId(), e.getName(), e.getDesc(), e.getIcon(), e.getToolType(), e.getUserId()))
                .toList();
    }
}
