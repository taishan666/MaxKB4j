package com.maxkb4j.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerateProblemDTO {
    private List<String> documentIdList;
    private List<String> paragraphIdList;
    private String modelId;
    private Integer number;
    private String prompt;
    private List<String> stateList;
}