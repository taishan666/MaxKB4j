package com.tarzan.maxkb4j.module.dataset.domain.dto;

import com.tarzan.maxkb4j.module.dataset.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ProblemEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParagraphDTO extends ParagraphEntity {

    private List<ProblemEntity> problemList;
}
