package com.maxkb4j.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class DocQuery {

    private String name;
    private Boolean isActive;
    private String hitHandlingMethod;
    private List<String> tags;
}
