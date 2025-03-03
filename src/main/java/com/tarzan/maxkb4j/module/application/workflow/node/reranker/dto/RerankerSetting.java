package com.tarzan.maxkb4j.module.application.workflow.node.reranker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RerankerSetting {
    @JsonProperty("top_n")
    private Integer topN;
    private Float similarity;
    @JsonProperty("max_paragraph_char_number")
    private Integer maxParagraphCharNumber;
}
