package com.maxkb4j.system.dto;

import lombok.Data;

@Data
public class AgentStatDTO {

    private String id;
    private String name;
    private Integer totalTokens;
    private Integer chatRecordCount;
    private Integer chatUserCount;
}
