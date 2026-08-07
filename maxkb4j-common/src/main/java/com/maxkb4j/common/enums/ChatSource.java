package com.maxkb4j.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ChatSource {
    //  "线上使用"
    ONLINE("ONLINE", "线上使用"),
    // "API调用"
    API_CALL("API_CALL", "API调用"),
    // "企业微信"
    ENTERPRISE_WECHAT("ENTERPRISE_WECHAT","企业微信"),
    //"微信公众号"
    WECHAT_PUBLIC_ACCOUNT("WECHAT_PUBLIC_ACCOUNT","微信公众号"),
    //"飞书"
    LARK("LARK","飞书"),
    //"钉钉"
    DING_TALK("DING_TALK","钉钉"),
    //"企业微信机器人"
    ENTERPRISE_WECHAT_ROBOT("ENTERPRISE_WECHAT_ROBOT","企业微信机器人"),
    //"触发器"
    TRIGGER("TRIGGER","触发器"),
    //"Slack"
    SLACK("SLACK","Slack");
    private final String key;
    private final String name;
}
