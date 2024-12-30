package com.tarzan.maxkb4j.module.dataset.vo;

import lombok.Data;

import java.util.UUID;

@Data
public class HitTestVO {
    private UUID paragraphId;
    private Double similarity;
    private Double comprehensiveScore;
}
