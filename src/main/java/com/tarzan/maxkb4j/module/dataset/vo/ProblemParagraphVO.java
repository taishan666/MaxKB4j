package com.tarzan.maxkb4j.module.dataset.vo;

import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProblemParagraphVO extends ProblemParagraphEntity {
    private String content;
    private String datasetId;
    private String documentId;
    private String paragraphId;
    private String problemId;
}
