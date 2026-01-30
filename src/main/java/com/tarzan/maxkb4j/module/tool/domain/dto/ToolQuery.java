package com.tarzan.maxkb4j.module.tool.domain.dto;

import com.tarzan.maxkb4j.common.domain.base.dto.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolQuery extends BaseQuery {
    private String scope;
    private String toolType;
    private Boolean isActive;
}
