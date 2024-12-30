package com.tarzan.maxkb4j.module.application.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApplicationStatisticsVO {
    @JsonProperty("chat_record_count")
    private Integer chatRecordCount;
    @JsonProperty("customer_added_count")
    private Integer customerAddedCount;
    @JsonProperty("customer_num")
    private Integer customerNum;
    private String day;
    @JsonProperty("star_num")
    private Integer starNum;
    @JsonProperty("tokens_num")
    private Integer tokensNum;
    @JsonProperty("trample_num")
    private Integer trampleNum;
}
