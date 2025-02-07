package com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class Condition {
    private List<String> field;

    @NotBlank(message = "Compare operation cannot be blank")
    private String compare;

    @NotBlank(message = "Value cannot be blank")
    private String value;
}
