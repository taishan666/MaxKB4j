package com.maxkb4j.common.domain.base.entity;

import lombok.Data;

@Data
public class ModelCredential {
    private String baseUrl;
    private String apiKey;
    private String modelPath;
    private String tokenizerPath;
}
