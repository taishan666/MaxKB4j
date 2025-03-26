package com.tarzan.maxkb4j.module.application.dto;

import lombok.Data;


@Data
public class ChatQueryDTO {

    private String keyword;
    private String startTime;
    private String endTime;
}
