package com.tarzan.maxkb4j.module.dataset.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class GenerateProblemDTO {
    private List<UUID> document_id_list;
    private List<UUID> paragraph_id_list;
    private UUID model_id;
    private String prompt;
}
