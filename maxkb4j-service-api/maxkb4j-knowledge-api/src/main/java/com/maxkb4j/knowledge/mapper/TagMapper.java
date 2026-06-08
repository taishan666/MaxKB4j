package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.vo.TagVO;

import java.util.List;

public interface TagMapper extends BaseMapper<TagEntity> {
    List<TagVO> listTags(String knowledgeId, String name);
}
