package com.tarzan.maxkb4j.module.dataset.vo;

import lombok.Data;

import java.util.List;

@Data
public class TextSegmentVO {

    private String name;

    private List<ParagraphSimpleVO> content;
}
