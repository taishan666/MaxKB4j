package com.maxkb4j.knowledge.dto;

import com.maxkb4j.knowledge.entity.ProblemEntity;
import lombok.Data;

import java.util.List;

@Data
public class ParagraphAddDTO  {
    private String title;
    private String content;
    private Integer position;
    private List<ProblemEntity> problemList;


}
