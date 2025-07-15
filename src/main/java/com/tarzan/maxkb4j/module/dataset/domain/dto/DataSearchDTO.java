package com.tarzan.maxkb4j.module.dataset.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class DataSearchDTO {
    private String  queryText;
    private String  searchMode;
    private Float  similarity;
    private Integer  topNumber;
    private Boolean  problemOptimization;
    private String rerankerModelId;
    private String  problemOptimizationPrompt;
    private List<String> excludeParagraphIds;
}
