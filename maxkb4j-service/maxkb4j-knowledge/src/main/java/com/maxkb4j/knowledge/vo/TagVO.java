package com.maxkb4j.knowledge.vo;

import lombok.Data;

@Data
public class TagVO {
    private String id;
    private String key;
    private String value;
    private Integer docCount;
}
