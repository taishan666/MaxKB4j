package com.maxkb4j.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class KnowledgeParams {

    private String actionId;
    private String knowledgeId;
    private DataSource dataSource;
    private Map<String, Object> knowledgeBase;
    private boolean debug;

}
