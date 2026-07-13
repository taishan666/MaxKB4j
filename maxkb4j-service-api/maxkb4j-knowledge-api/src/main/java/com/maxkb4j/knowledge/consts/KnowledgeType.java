package com.maxkb4j.knowledge.consts;

/**
 * 知识库类型常量。
 *
 * <p>采用 {@code final class} + 私有构造，避免“常量接口”反模式（Effective Java #22）。</p>
 */
public final class KnowledgeType {

    private KnowledgeType() {
    }

    /** 通用类型 */
    public static final int BASE = 0;
    /** web 站点类型 */
    public static final int WEB = 1;
    /** 工作流类型 */
    public static final int WORKFLOW = 2;

}
