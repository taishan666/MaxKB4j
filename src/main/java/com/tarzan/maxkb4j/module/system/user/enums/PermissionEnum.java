package com.tarzan.maxkb4j.module.system.user.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum PermissionEnum {
    USER_CREATE("USER","CREATE", List.of("ADMIN","USER")),
    USER_READ("USER","READ", List.of("ADMIN","USER")),
    USER_EDIT("USER","EDIT", List.of("ADMIN","USER")),
    USER_DELETE("USER","DELETE", List.of("ADMIN","USER")),
    DATASET_CREATE("DATASET","CREATE", List.of("ADMIN","USER")),
    DATASET_READ("DATASET","READ", List.of("ADMIN","USER")),
    DATASET_EDIT("DATASET","EDIT", List.of("ADMIN","USER")),
    DATASET_DELETE("DATASET","DELETE", List.of("ADMIN","USER")),
    APPLICATION_READ("APPLICATION","READ", List.of("ADMIN","USER")),
    APPLICATION_CREATE("APPLICATION","CREATE", List.of("ADMIN","USER")),
    APPLICATION_DELETE("APPLICATION","DELETE", List.of("ADMIN","USER")),
    APPLICATION_EDIT("APPLICATION","EDIT", List.of("ADMIN","USER")),
    SETTING_READ("SETTING","READ", List.of("ADMIN")),
    SETTING_EDIT("SETTING","EDIT", List.of("ADMIN")),
    MODEL_READ("MODEL","READ", List.of("ADMIN","USER")),
    MODEL_EDIT("MODEL","EDIT", List.of("ADMIN","USER")),
    MODEL_DELETE("MODEL","DELETE", List.of("ADMIN","USER")),
    MODEL_CREATE("MODEL","CREATE", List.of("ADMIN","USER")),
    TEAM_READ("TEAM","READ", List.of("ADMIN","USER")),
    TEAM_CREATE("TEAM","CREATE", List.of("ADMIN","USER")),
    TEAM_DELETE("TEAM","DELETE", List.of("ADMIN","USER")),
    TEAM_EDIT("TEAM","EDIT", List.of("ADMIN","USER"));

    private final String group;
    private final String operate;
    private final List<String> roles;

    PermissionEnum(String group, String operate, List<String> roles) {
        this.group = group;
        this.operate = operate;
        this.roles = roles;
    }

    /**
     * 返回所有权限枚举的列表。
     *
     * @return 所有权限枚举的列表。
     */
    public static List<PermissionEnum> getAllPermissions() {
        return Arrays.stream(values())
                .collect(Collectors.toList());
    }
}
