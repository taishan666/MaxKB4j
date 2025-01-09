package com.tarzan.maxkb4j.module.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ModelDTO {

    private String name;

    @JsonProperty("model_type")
    private String modelType;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("permission_type")
    private String permissionType;
}
