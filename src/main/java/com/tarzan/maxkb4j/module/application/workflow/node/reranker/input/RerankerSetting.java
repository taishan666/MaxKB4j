package com.tarzan.maxkb4j.module.application.workflow.node.reranker.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RerankerSetting {
    private Integer topN;
    private Float similarity;
    private Integer maxParagraphCharNumber;
}
