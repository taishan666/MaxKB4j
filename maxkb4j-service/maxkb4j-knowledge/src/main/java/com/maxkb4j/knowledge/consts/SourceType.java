package com.maxkb4j.knowledge.consts;

/**
 * 嵌入来源类型常量。
 *
 * <p>采用 {@code final class} + 私有构造，避免“常量接口”反模式（Effective Java #22）。</p>
 */
public final class SourceType {

    private SourceType() {
    }

    /** 问题来源 */
    public static final int PROBLEM = 0;
    /** 段落来源 */
    public static final int PARAGRAPH = 1;

}
