package com.tarzan.maxkb4j.module.application.domian.vo;

import lombok.Data;

@Data
public class ApplicationListVO {

    private String id;

    private String name;

    private String desc;

    private String icon;

    private String resourceType="application";

    private Boolean isPublish;
}
