package com.tarzan.maxkb4j.module.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class ChatQueryDTO {

    @JsonProperty("abstract")
    private String keyword;
    private String startTime;
    private String endTime;
}
