package com.tarzan.maxkb4j.core.workflow.node.reranker.out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RerankResult {
    private String pageContent;
    private Map<String,Object> metadata;
}
