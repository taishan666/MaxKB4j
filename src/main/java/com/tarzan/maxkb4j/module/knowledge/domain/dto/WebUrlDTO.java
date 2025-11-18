package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class WebUrlDTO {
    private String selector;
    private List<String> sourceUrlList;
}
