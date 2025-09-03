package com.tarzan.maxkb4j.module.tool.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolDebugField extends ToolInputField {

    private Object value;
}
