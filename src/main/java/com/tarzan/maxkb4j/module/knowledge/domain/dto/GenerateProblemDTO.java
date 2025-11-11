package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerateProblemDTO {
    private List<String> documentIdList;
    private List<String> paragraphIdList;
    private String modelId;
    private String prompt;
    private List<String> stateList;
}