package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ParagraphSimple {
    private String title;
    private String content;
    private List<String> problemList;
}
