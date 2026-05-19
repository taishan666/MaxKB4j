package com.maxkb4j.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DualKeywordResult {

    private List<String> highLevelKeywords;
    private List<String> lowLevelKeywords;
}