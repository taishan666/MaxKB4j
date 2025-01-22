package com.tarzan.maxkb4j.module.dataset.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerateProblemDTO {
    private List<String> document_id_list;
    private List<String> paragraph_id_list;
    private String model_id;
    private String prompt;
}
