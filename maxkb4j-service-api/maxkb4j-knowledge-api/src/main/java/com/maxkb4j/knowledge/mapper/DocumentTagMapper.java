package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.TagEntity;

import java.util.List;

public interface DocumentTagMapper extends BaseMapper<DocumentTagEntity> {
    List<TagEntity> listTagsByDocId(String docId);
}
