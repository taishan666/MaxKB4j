package com.maxkb4j.tool.dto;

import com.maxkb4j.tool.entity.ToolEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolDTO extends ToolEntity {
    private List<ToolInputField> debugFieldList;
}
