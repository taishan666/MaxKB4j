package com.tarzan.maxkb4j.module.dataset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProblemDTO extends ProblemEntity {
    private String paragraphId;
    @JsonProperty("document_id")
    private String documentId;
}
