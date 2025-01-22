package com.tarzan.maxkb4j.module.dataset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DatasetBatchHitHandlingDTO {
    @JsonProperty("hit_handling_method")
    private String hitHandlingMethod;
    @JsonProperty("directly_return_similarity")
    private Double directlyReturnSimilarity;
    @JsonProperty("id_list")
    private List<String> idList;
}
