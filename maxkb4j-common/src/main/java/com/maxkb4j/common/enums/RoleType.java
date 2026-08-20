package com.maxkb4j.common.enums;


import com.maxkb4j.common.constant.RoleConst;
import lombok.Getter;

@Getter
public enum RoleType {
    ADMIN(RoleConst.ADMIN,"系统管理员"),
    WORKSPACE_MANAGE(RoleConst.WORKSPACE_MANAGE,"工作空间管理员"),
    USER(RoleConst.USER,"普通用户"),
    EXTENDS_ADMIN(RoleConst.EXTENDS_ADMIN,"继承超级管理员"),
    EXTENDS_WORKSPACE_MANAGE(RoleConst.EXTENDS_WORKSPACE_MANAGE,"继承工作空间管理员"),
    EXTENDS_USER(RoleConst.EXTENDS_USER,"继承普通用户"),
    CHAT_ANONYMOUS_USER(RoleConst.CHAT_ANONYMOUS_USER,"对话匿名用户"),
    CHAT_USER(RoleConst.CHAT_USER,"对话用户"),
    ;
    private final String type;
    private final String name;
    RoleType(String type, String name) {
        this.type = type;
        this.name = name;
    }
}


