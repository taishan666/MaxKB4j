package com.tarzan.maxkb4j.module.functionlib.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FunctionDebugField extends FunctionInputField {

    private Object value;
}
