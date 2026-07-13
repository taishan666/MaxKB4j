package com.maxkb4j.knowledge.consts;

/**
 * 检索类型的字符串标识：用于前端入参 / 存储字段与 {@link com.maxkb4j.knowledge.retrieval.SearchMode}
 * 枚举之间的映射（见 {@code DataRetriever}）。
 *
 * <p>采用 {@code final class} + 私有构造，避免“常量接口”反模式（Effective Java #22）：
 * 接口应表达类型契约，而非仅用于导出常量。</p>
 */
public final class SearchType {

    private SearchType() {
    }

    /** 向量检索 */
    public static final String EMBEDDING = "embedding";
    /** 全文检索 */
    public static final String FULL_TEXT = "keywords";
    /** 混合检索 */
    public static final String HYBRID = "hybrid";

}
