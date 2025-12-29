package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ParagraphSimple {
    private String title;
    private String content;
    private List<String> problemList;
}
