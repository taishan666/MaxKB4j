package com.maxkb4j.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 问题更新入参。
 *
 * @author tarzan
 */
@Data
public class ProblemUpdateDTO {

    @NotBlank(message = "问题内容不能为空")
    private String content;
}
