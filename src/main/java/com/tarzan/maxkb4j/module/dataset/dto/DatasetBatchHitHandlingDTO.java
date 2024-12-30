package com.tarzan.maxkb4j.module.dataset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DatasetBatchHitHandlingDTO {
    @JsonProperty("hit_handling_method")
    private String hitHandlingMethod;
    @JsonProperty("directly_return_similarity")
    private Double directlyReturnSimilarity;
    @JsonProperty("id_list")
    private List<UUID> idList;
}
