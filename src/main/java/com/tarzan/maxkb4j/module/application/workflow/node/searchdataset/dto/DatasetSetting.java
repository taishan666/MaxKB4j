package com.tarzan.maxkb4j.module.application.workflow.node.searchdataset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DatasetSetting {

    @JsonProperty("top_n")
    private Integer topN;
    private Float similarity;
    @JsonProperty("search_mode")
    private String searchMode;
    @JsonProperty("max_paragraph_char_number")
    private Integer maxParagraphCharNumber;
}
