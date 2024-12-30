package com.tarzan.maxkb4j.module.dataset.dto;

import lombok.Data;

@Data
public class HitTestDTO {
    private String  query_text;
    private String  search_mode;
    private Double  similarity;
    private Double  top_number;
}
