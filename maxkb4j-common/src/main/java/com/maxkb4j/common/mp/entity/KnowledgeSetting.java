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

}
