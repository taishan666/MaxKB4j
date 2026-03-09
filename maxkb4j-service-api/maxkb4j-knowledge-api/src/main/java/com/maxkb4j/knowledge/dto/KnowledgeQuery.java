package com.maxkb4j.knowledge.dto;

import com.maxkb4j.common.domain.base.dto.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeQuery extends BaseQuery {

    private List<String> targetIds;
    private Boolean isAdmin=false;
    private Integer type;
}
