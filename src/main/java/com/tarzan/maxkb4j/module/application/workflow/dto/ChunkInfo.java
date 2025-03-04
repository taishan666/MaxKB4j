package com.tarzan.maxkb4j.module.application.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

//其他参数
@AllArgsConstructor
@Data
@NoArgsConstructor
public class ChunkInfo {
    private String nodeType;
    private String runtimeNodeId;
    private String viewType;
    private Map<String, Object> childNode;
    private Boolean nodeIsEnd;
    private String realNodeId;
}
