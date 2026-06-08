package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.mapper.TagMapper;
import com.maxkb4j.knowledge.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagService extends ServiceImpl<TagMapper, TagEntity> implements ITagService{

    private final DocumentTagService documentTagService;
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
}
