package com.maxkb4j.knowledge.vo;

import com.maxkb4j.knowledge.entity.TagEntity;
import lombok.Data;

import java.util.List;

@Data
public class TagListVO {
    private String key;
    private List<TagEntity> values;
}
