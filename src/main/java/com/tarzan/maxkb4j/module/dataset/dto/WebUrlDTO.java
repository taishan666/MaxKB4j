package com.tarzan.maxkb4j.module.dataset.dto;

import lombok.Data;

import java.util.List;

@Data
public class WebUrlDTO {
    private String selector;
    private List<String> sourceUrlList;
}
