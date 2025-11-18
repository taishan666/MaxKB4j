package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class DataSearchDTO {
    private String  queryText;
    private String  searchMode;
    private Float  similarity;
    private Integer  topNumber;
    private List<String> excludeParagraphIds;
}
