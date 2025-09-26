package com.tarzan.maxkb4j.common.base.dto;

import lombok.Data;

import java.util.Set;

@Data
public class TSVector {
    private Set<SearchIndex> searchVector;
}
