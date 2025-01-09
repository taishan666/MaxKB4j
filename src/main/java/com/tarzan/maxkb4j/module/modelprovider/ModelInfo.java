package com.tarzan.maxkb4j.module.modelprovider;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ModelInfo {

    private String name;
    private String desc;
    @JsonProperty("model_type")
    private String modelType;

}
