package com.tarzan.maxkb4j.module.system.setting.enums;

import lombok.Getter;

@Getter
public enum SettingType {

    /* 邮件 */
    Email(0),
    /*密匙*/
    KEY(1),
    /*主题显示*/
    DISPLAY(2);

    private final int type;
    SettingType(int type) {
        this.type = type;
    }
}
