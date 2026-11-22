package com.maxkb4j.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ChatUserType {

    /* 匿名用户 */
    ANONYMOUS_USER("ANONYMOUS_USER", "匿名用户"),
    /* 对话用户 */
    CHAT_USER("CHAT_USER", "对话用户"),
    /*系统API_KEY*/
    SYSTEM_API_KEY("SYSTEM_API_KEY", "系统API_KEY"),
    /* 应用API_KEY*/
    APPLICATION_API_KEY("APPLICATION_API_KEY", "应用API_KEY"),
    /*平台用户*/
    PLATFORM_USER("PLATFORM_USER", "平台用户"),
    /*触发器（事件触发器的机器调用方，非真实客户）*/
    TRIGGER_USER("TRIGGER_USER", "触发器");

    private final String key;
    private final String name;
}
