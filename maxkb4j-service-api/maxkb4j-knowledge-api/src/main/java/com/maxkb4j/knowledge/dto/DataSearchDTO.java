package com.maxkb4j.knowledge.dto;

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
