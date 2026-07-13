package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.vo.DocumentTagVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DocumentTagMapper extends BaseMapper<DocumentTagEntity> {
    List<TagEntity> listTags(String docId,String name);

    List<DocumentTagVO> listTagsByDocIds(@Param("docIds") List<String> docIds);
}
