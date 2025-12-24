package com.tarzan.maxkb4j.module.application.domain.dto;

import lombok.Data;


@Data
public class ChatQueryDTO {

    private String summary;
    private String startTime;
    private String endTime;
    private Integer minStar;
    private Integer minTrample;
}
