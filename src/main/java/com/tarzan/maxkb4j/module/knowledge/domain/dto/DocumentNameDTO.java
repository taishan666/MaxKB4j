package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class DocumentNameDTO {
    private String name;
    private List<ParagraphSimpleDTO> paragraphs;
    private String sourceFileId;
}
