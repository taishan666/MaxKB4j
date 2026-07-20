package com.maxkb4j.knowledge.vo;

import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProblemParagraphVO extends ProblemParagraphEntity {
    private String content;
}
