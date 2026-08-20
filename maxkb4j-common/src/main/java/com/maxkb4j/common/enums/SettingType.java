package com.maxkb4j.common.enums;

import lombok.Getter;

@Getter
public enum SettingType {

    /* 邮件 */
    Email(0),
    /*密匙*/
    KEY(1),
    /*主题显示*/
    DISPLAY(2),
    /* 许可证 */
    License(3),
    /*认证设置*/
    ADMIN_AUTH_SETTING(4),
    /*认证配置*/
    ADMIN_AUTH_CONFIG(5),
    /*平台来源*/
    ADMIN_PLATFORM_SOURCE(6),
    /*认证配置*/
    CHAT_AUTH_CONFIG(7),
    /*平台来源*/
    CHAT_PLATFORM_SOURCE(8);

    private final int type;
    SettingType(int type) {
        this.type = type;
    }
}
