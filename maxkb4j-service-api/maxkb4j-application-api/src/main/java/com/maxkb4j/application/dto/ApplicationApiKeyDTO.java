package com.maxkb4j.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApplicationApiKeyDTO {
    private String secretKey;
    private Boolean isActive;
    private String applicationId;
    private String userId;
    private Boolean allowCrossDomain;
    private List<String> crossDomainList;
}
