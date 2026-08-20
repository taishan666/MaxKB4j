package com.maxkb4j.system.dto;

import lombok.Data;

@Data
public class DailyStatDTO {
    private Integer starNum;

    private Integer trampleNum;

    private Integer tokensNum;

    private Integer chatRecordCount;

    private Integer customerNum;

    private String day;

    private Integer customerAddedCount;
}
