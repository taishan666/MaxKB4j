package com.maxkb4j.user.vo;

import lombok.Data;


@Data
public class PermissionVO {
    private String id;
    private String type;
    private String userId;
    private String operate;
}
