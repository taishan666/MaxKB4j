package com.tarzan.maxkb4j.module.dataset.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ProblemVO extends ProblemEntity {

    @JsonProperty("paragraph_count")
    private Integer paragraphCount;

    private String documentId;
    private String paragraphId;

}
