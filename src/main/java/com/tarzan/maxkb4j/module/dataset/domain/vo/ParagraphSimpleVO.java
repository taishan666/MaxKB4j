package com.tarzan.maxkb4j.module.dataset.domain.vo;

import lombok.Data;

@Data
public class ParagraphSimpleVO {

    private String title;

    private String content;

    public ParagraphSimpleVO(String content) {
        this.content = content;
    }
}
