package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import com.tarzan.maxkb4j.common.base.dto.BaseQuery;
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
