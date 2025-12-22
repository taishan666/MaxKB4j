package com.tarzan.maxkb4j.module.knowledge.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class TextSegmentVO {

    private String name;

    private List<ParagraphSimpleVO> content;

    private String sourceFileId;
}
