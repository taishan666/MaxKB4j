package com.tarzan.maxkb4j.module.application.domian.vo;

import lombok.Data;

@Data
public class ApplicationStatisticsVO {
    private Integer chatRecordCount;
    private Integer customerAddedCount;
    private Integer customerNum;
    private String day;
    private Integer starNum;
    private Integer tokensNum;
    private Integer trampleNum;
}
