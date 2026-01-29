package com.tarzan.maxkb4j.module.application.domain.vo;

import lombok.Data;

@Data
public class ApplicationListVO {
    private String id;
    private String name;
    private String desc;
    private String icon;
    private Boolean isPublish;
}
