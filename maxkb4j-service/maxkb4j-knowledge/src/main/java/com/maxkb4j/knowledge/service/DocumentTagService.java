package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.mapper.DocumentTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class DocumentTagService extends ServiceImpl<DocumentTagMapper,DocumentTagEntity> implements IDocumentTagService{
    @Override
    public List<TagEntity> listTagsByDocId(String docId) {
        return baseMapper.listTagsByDocId(docId);
    }

    @Override
    public Map<String, List<TagEntity>> listTagsByDocIds(List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = baseMapper.listTagsByDocIds(docIds);
        Map<String, List<TagEntity>> result = new LinkedHashMap<>(docIds.size());
        for (Map<String, Object> row : rows) {
            String documentId = (String) row.get("document_id");
            TagEntity tag = new TagEntity();
            tag.setId((String) row.get("id"));
            tag.setKey((String) row.get("key"));
            tag.setValue((String) row.get("value"));
            result.computeIfAbsent(documentId, k -> new java.util.ArrayList<>()).add(tag);
        }
        return result;
    }
}
