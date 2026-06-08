package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.TagEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface DocumentTagMapper extends BaseMapper<DocumentTagEntity> {
    List<TagEntity> listTagsByDocId(String docId);

    List<Map<String, Object>> listTagsByDocIds(@Param("docIds") List<String> docIds);
}
