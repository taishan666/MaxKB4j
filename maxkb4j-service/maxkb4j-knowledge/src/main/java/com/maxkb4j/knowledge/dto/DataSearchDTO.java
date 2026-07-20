package com.maxkb4j.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class DataSearchDTO {
    @NotBlank(message = "查询内容不能为空")
    private String  queryText;
    private String  searchMode;
    private Float  similarity;
    private Integer  topNumber;
    private List<String> excludeParagraphIds;
}
