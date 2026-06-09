package com.maxkb4j.application.service.resolver;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.system.entity.SourceResource;
import com.maxkb4j.system.strategy.SourceResourceResolver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationSourceResolver implements SourceResourceResolver {

    private final ApplicationMapper applicationMapper;

    @Override
    public String resourceType() {
        return ResourceType.APPLICATION;
    }

    @Override
    public List<SourceResource> resolve(Collection<String> ids, String resourceName, List<String> userIdFilter) {
        List<ApplicationEntity> apps = applicationMapper.selectList(Wrappers.<ApplicationEntity>lambdaQuery()
                .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc, ApplicationEntity::getIcon, ApplicationEntity::getType, ApplicationEntity::getUserId)
                .like(StringUtils.isNotBlank(resourceName), ApplicationEntity::getName, resourceName)
                .in(userIdFilter != null, ApplicationEntity::getUserId, userIdFilter)
                .in(ApplicationEntity::getId, ids));
        return apps.stream()
                .map(e -> new SourceResource(e.getId(), e.getName(), e.getDesc(), e.getIcon(), e.getType(), e.getUserId()))
                .toList();
    }
}
