package com.maxkb4j.model.service.resolver;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.mapper.ModelMapper;
import com.maxkb4j.system.entity.SourceResource;
import com.maxkb4j.system.strategy.SourceResourceResolver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ModelSourceResolver implements SourceResourceResolver {

    private final ModelMapper modelMapper;

    @Override
    public String resourceType() {
        return ResourceType.MODEL;
    }

    @Override
    public List<SourceResource> resolve(Collection<String> ids, String resourceName, List<String> userIdFilter) {
        List<ModelEntity> list = modelMapper.selectList(Wrappers.<ModelEntity>lambdaQuery()
                .select(ModelEntity::getId, ModelEntity::getName, ModelEntity::getModelType, ModelEntity::getProvider, ModelEntity::getUserId)
                .like(StringUtils.isNotBlank(resourceName), ModelEntity::getName, resourceName)
                .in(userIdFilter != null, ModelEntity::getUserId, userIdFilter)
                .in(ModelEntity::getId, ids));
        return list.stream()
                .map(e -> new SourceResource(e.getId(), e.getName(), "", e.getProvider(), e.getModelType(), e.getUserId()))
                .toList();
    }
}
