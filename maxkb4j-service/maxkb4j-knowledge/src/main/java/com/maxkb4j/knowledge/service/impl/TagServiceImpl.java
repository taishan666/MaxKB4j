package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.mapper.TagMapper;
import com.maxkb4j.knowledge.service.IDocumentTagService;
import com.maxkb4j.knowledge.service.ITagService;
import com.maxkb4j.knowledge.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, TagEntity> implements ITagService {

    private final IDocumentTagService documentTagService;
    @Override
    @Transactional
    public Boolean deleteTagId(String tagId) {
        documentTagService.lambdaUpdate().eq(DocumentTagEntity::getTagId,tagId).remove();
        return this.removeById(tagId);
    }

    @Override
    @Transactional
    public Boolean batchDelete(List<String> tagIds) {
        documentTagService.lambdaUpdate().in(DocumentTagEntity::getTagId,tagIds).remove();
        return this.removeByIds(tagIds);
    }

    @Override
    public List<TagVO> listTags(String id, String name) {
        return baseMapper.listTags(id,name);
    }

    @Override
    public Boolean docsDelete(String tagId, List<String> docIds) {
        return documentTagService.lambdaUpdate().eq(DocumentTagEntity::getTagId,tagId).in(DocumentTagEntity::getDocumentId,docIds).remove();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addTags(String knowledgeId, List<TagEntity> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return true;
        }
        Set<String> keys = tags.stream().map(TagEntity::getKey).collect(Collectors.toSet());
        Set<String> values = tags.stream().map(TagEntity::getValue).collect(Collectors.toSet());
        Set<String> existTags = this.lambdaQuery()
                .select(TagEntity::getKey, TagEntity::getValue)
                .eq(TagEntity::getKnowledgeId, knowledgeId)
                .in(TagEntity::getKey, keys)
                .in(TagEntity::getValue, values)
                .list()
                .stream()
                .map(tag -> tag.getKey() + ":" + tag.getValue())
                .collect(Collectors.toSet());
        Set<String> addTags = new HashSet<>();
        List<TagEntity> saveTags = tags.stream()
                .filter(tag -> !existTags.contains(tag.getKey() + ":" + tag.getValue()) && addTags.add(tag.getKey() + ":" + tag.getValue()))
                .peek(tag -> tag.setKnowledgeId(knowledgeId))
                .toList();
        if (saveTags.isEmpty()) {
            return true;
        }
        return this.saveBatch(saveTags);
    }
}
