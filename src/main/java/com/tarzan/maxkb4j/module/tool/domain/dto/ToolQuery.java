package com.tarzan.maxkb4j.module.tool.domain.dto;

import com.tarzan.maxkb4j.common.base.dto.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolQuery extends Query {
    private String scope;
    private String toolType;
}
