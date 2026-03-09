package com.maxkb4j.knowledge.vo;

import com.maxkb4j.knowledge.entity.ProblemEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ProblemVO extends ProblemEntity {

    private Integer paragraphCount;
    private String documentId;
    private String paragraphId;

}
