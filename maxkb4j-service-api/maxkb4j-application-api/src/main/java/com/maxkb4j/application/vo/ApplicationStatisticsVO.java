package com.maxkb4j.application.vo;

import lombok.Data;

@Data
public class ApplicationStatisticsVO {
    private Integer chatRecordCount;
    private Integer customerAddedCount;
    private Integer customerNum;
    private Integer starNum;
    private Integer tokensNum;
    private Integer trampleNum;
    private String day;
    private Integer tokenUsage;
    private String userName;
}
