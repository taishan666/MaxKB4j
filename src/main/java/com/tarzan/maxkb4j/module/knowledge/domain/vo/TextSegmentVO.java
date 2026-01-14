package com.tarzan.maxkb4j.module.knowledge.domain.vo;

import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphSimple;
import lombok.Data;

import java.util.List;

@Data
public class TextSegmentVO {

    private String name;

    private List<ParagraphSimple> content;

    private String sourceFileId;
}
