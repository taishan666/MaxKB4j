package com.maxkb4j.common.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class DualKeywords {

    private List<String> highLevelKeywords;
    private List<String> lowLevelKeywords;

}
