package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import lombok.Data;

import java.util.List;

@Data
public class ParagraphAddDTO  {
    private String title;
    private String content;
    private Integer position;
    private List<ProblemEntity> problemList;


}
