package com.maxkb4j.knowledge.service.resolver;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.system.entity.SourceResource;
import com.maxkb4j.system.strategy.SourceResourceResolver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KnowledgeSourceResolver implements SourceResourceResolver {

    private final KnowledgeMapper knowledgeMapper;

    @Override
    public String resourceType() {
        return ResourceType.KNOWLEDGE;
    }

    @Override
    public List<SourceResource> resolve(Collection<String> ids, String resourceName, List<String> userIdFilter) {
        List<KnowledgeEntity> list = knowledgeMapper.selectList(Wrappers.<KnowledgeEntity>lambdaQuery()
                .select(KnowledgeEntity::getId, KnowledgeEntity::getName, KnowledgeEntity::getDesc, KnowledgeEntity::getType, KnowledgeEntity::getUserId)
                .like(StringUtils.isNotBlank(resourceName), KnowledgeEntity::getName, resourceName)
                .in(userIdFilter != null, KnowledgeEntity::getUserId, userIdFilter)
                .in(KnowledgeEntity::getId, ids));
        return list.stream()
                .map(e -> new SourceResource(e.getId(), e.getName(), e.getDesc(), "", String.valueOf(e.getType()), e.getUserId()))
                .toList();
    }
}
