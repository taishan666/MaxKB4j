package com.maxkb4j.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class DatasetBatchHitHandlingDTO {
    @NotBlank(message = "命中处理方式不能为空")
    private String hitHandlingMethod;
    private Double directlyReturnSimilarity;
    private List<String> idList;
}
