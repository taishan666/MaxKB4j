package com.tarzan.maxkb4j.module.dataset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DatasetBatchHitHandlingDTO {
    private String hitHandlingMethod;
    private Double directlyReturnSimilarity;
    private List<String> idList;
}
