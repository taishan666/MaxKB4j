package com.maxkb4j.knowledge.vo;

import lombok.Data;

@Data
public class ParagraphPageVO {
    private String id;
    private String title;
    private String content;
    private Boolean isActive;
    private Integer position;
}
