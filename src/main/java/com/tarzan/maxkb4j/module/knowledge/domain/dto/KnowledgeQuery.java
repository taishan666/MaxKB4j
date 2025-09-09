package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import com.tarzan.maxkb4j.core.common.dto.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeQuery extends Query {

    private List<String> targetIds;
    private Boolean isAdmin=false;
}
