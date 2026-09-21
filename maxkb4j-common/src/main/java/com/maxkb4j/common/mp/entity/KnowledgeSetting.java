package com.maxkb4j.common.mp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class KnowledgeSetting {

    private Boolean onDemandEnable;
    private Integer topN;
    private Integer maxParagraphCharNumber;
    private String searchMode;
    private Float similarity;
    private Boolean fallbackEnable;
    private String fallbackResponse;
    /**
     * 是否启用检索结果重排（rerank）；开启后召回超采样，由 RerankStep 精排截断。
     */
    private Boolean rerankEnable;
    /**
     * 重排模型 ID（ScoringModel）；缺省时 rerankEnable 不生效。
     */
    private String rerankModelId;
    /**
     * 重排后保留的条数；缺省回退 topN。
     */
    private Integer rerankTopN;

}
