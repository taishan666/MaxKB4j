package com.tarzan.maxkb4j.module.application.entity;

import lombok.Data;

@Data
public class DatasetSetting {

    private Integer topN;
    private Integer maxParagraphCharNumber;
    private String searchMode;
    private Float similarity;
    private NoReferencesSetting noReferencesSetting;

}
