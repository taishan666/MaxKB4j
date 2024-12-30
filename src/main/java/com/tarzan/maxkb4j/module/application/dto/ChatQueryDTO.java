package com.tarzan.maxkb4j.module.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class ChatQueryDTO {

    @JsonProperty("abstract")
    private String keyword;
    @JsonProperty("start_time")
    private String startTime;
    @JsonProperty("end_time")
    private String endTime;
}
