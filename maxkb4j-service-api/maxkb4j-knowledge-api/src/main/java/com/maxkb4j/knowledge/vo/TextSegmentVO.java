package com.maxkb4j.knowledge.vo;

import com.maxkb4j.knowledge.dto.ParagraphSimple;
import lombok.Data;

import java.util.List;

@Data
public class TextSegmentVO {

    private String name;

    private List<ParagraphSimple> content;

    private String sourceFileId;
}
