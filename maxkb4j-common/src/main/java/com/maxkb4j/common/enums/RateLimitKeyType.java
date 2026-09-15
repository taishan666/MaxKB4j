package com.maxkb4j.common.enums;

/**
 * 限流维度类型
 */
public enum RateLimitKeyType {

    /** 按客户端 IP 限流 */
    IP,

    /** 按登录用户限流（未登录时降级为 IP） */
    USER,

    /** 全局限流（所有请求共享计数） */
    GLOBAL,

    /** 自定义 SpEL 表达式限流 */
    CUSTOM
}
