package com.tarzan.maxkb4j.module.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ModelCredential {
    @JsonProperty("api_base")
    private String apiBase;
    @JsonProperty("api_key")
    private String apiKey;
}
