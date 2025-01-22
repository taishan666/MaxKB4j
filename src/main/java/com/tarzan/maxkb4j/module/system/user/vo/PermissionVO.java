package com.tarzan.maxkb4j.module.system.user.vo;

import lombok.Data;

import java.util.UUID;

@Data
public class PermissionVO {
    private UUID id;
    private String type;
    private UUID userId;
    private String operate;
}
