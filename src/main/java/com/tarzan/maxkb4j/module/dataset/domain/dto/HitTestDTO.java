package com.tarzan.maxkb4j.module.dataset.domain.dto;

import lombok.Data;

@Data
public class HitTestDTO {
    private String  queryText;
    private String  searchMode;
    private Float  similarity;
    private Integer  topNumber;
    private Boolean  problemOptimization;
    private String rerankerModelId;
    private String  problemOptimizationPrompt;
}
