package com.tarzan.maxkb4j.module.application.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApplicationPublicAccessClientStatisticsVO {
    private String day;
    @JsonProperty("customer_added_count")
    private Integer customerAddedCount;
}
