package com.maxkb4j.knowledge.vo;

import lombok.Data;

import java.util.List;

@Data
public class TagListVO {
    private String key;
    private List<TagVO> values;
}
