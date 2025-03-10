package com.tarzan.maxkb4j.module.dataset.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResult {
    private String content;
    private float score;

}