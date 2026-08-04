package com.maxkb4j.application.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ApplicationListVO {
    private String id;
    private String name;
    private String desc;
    private String type;
    private String icon;
    private String nickname;
    private Date createTime;
    private Date updateTime;
    private Boolean isPublish;
}
