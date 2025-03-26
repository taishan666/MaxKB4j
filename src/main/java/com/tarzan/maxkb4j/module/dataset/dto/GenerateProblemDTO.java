package com.tarzan.maxkb4j.module.dataset.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerateProblemDTO {
    private List<String> documentIdList;
    private List<String> paragraphIdList;
    private String modelId;
    private String prompt;
}