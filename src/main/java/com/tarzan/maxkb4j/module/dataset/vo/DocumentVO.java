package com.tarzan.maxkb4j.module.dataset.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.dataset.entity.DocumentEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DocumentVO extends DocumentEntity {
    @JsonProperty("paragraph_count")
    private Integer paragraphCount;
}
