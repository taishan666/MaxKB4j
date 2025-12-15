package com.tarzan.maxkb4j.module.application.domian.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@RequiredArgsConstructor
@Data
public class KnowledgeSetting {

    private Integer topN;
    private Integer maxParagraphCharNumber;
    private String searchMode;
    private Float similarity;
    private NoReferencesSetting noReferencesSetting;

}
