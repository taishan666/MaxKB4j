package com.maxkb4j.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProblemDTO {
    @NotBlank(message = "内容不能为空")
    private String content;
}
