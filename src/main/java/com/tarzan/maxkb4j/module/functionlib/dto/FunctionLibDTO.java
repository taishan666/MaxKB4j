package com.tarzan.maxkb4j.module.functionlib.dto;

import com.tarzan.maxkb4j.module.functionlib.entity.FunctionLibEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FunctionLibDTO extends FunctionLibEntity {
    private List<FunctionDebugField> debugFieldList;
}
