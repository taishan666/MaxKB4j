package com.tarzan.maxkb4j.module.application.domain.vo;

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
}
