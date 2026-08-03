package com.maxkb4j.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ModelQuery {
    private String name;
    private String modelName;
    private String provider;
    private String modelType;
    private String createUser;
    private List<String> targetIds;
    private Boolean isAdmin=false;
}
