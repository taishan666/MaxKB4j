package com.tarzan.maxkb4j.module.tool.domain.dto;

import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolDTO extends ToolEntity {
    private List<ToolInputField> debugFieldList;
}
