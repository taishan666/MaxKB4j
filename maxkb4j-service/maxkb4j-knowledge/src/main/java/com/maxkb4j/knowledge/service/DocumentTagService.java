package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.mapper.DocumentTagMapper;
import com.maxkb4j.knowledge.vo.DocumentTagVO;
import com.maxkb4j.knowledge.vo.TagListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DocumentTagService extends ServiceImpl<DocumentTagMapper,DocumentTagEntity> implements IDocumentTagService{
    @Override
    public List<TagListVO> listTags(String docId,String name) {
        List<TagListVO> tagList = new ArrayList<>();
        List<TagEntity> tags = baseMapper.listTags(docId,name);
        Map<String, List<TagEntity>> groupedTags = tags.stream().collect(Collectors.groupingBy(TagEntity::getKey));
        groupedTags.forEach((key, value) -> {
            TagListVO tagListVO = new TagListVO();
            tagListVO.setKey(key);
            tagListVO.setValues(value);
            tagList.add(tagListVO);
        });
        return tagList;
    }

    @Override
    public Map<String, List<TagEntity>> listTagsByDocIds(List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return Collections.emptyMap();
        }
        List<DocumentTagVO> rows = baseMapper.listTagsByDocIds(docIds);
        Map<String, List<TagEntity>> result = new LinkedHashMap<>(docIds.size());
        for (DocumentTagVO row : rows) {
            TagEntity tag = new TagEntity();
            tag.setId(row.getId());
            tag.setKey(row.getKey());
            tag.setValue(row.getValue());
            result.computeIfAbsent(row.getDocumentId(), k -> new ArrayList<>()).add(tag);
        }
        return result;
    }
}
