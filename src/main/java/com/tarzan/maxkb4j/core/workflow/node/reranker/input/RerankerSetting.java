package com.tarzan.maxkb4j.core.workflow.node.reranker.input;

import lombok.Data;

@Data
public class RerankerSetting {
    private Integer topN;
    private Float similarity;
    private Integer maxParagraphCharNumber;
}
