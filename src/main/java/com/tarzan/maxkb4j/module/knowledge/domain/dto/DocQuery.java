package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import lombok.Data;

@Data
public class DocQuery {

    private String name;
    private Boolean isActive;
    private String hitHandlingMethod;
}
