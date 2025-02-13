package com.tarzan.maxkb4j.module.dataset.dto;

import lombok.Data;

@Data
public class HitTestDTO {
    private String  query_text;
    private String  search_mode;
    private Float  similarity;
    private Integer  top_number;
    private Boolean  problem_optimization;
    private String reranker_model_id;
    private String  problem_optimization_prompt;
}
