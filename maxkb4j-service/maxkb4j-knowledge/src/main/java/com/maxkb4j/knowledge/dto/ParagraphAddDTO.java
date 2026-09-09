package com.maxkb4j.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ParagraphAddDTO  {
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    private Integer position;
    private List<ProblemDTO> problemList;


}
