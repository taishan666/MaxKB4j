package com.maxkb4j.knowledge.consts;

public interface HitHandlingMethod {
    /** 命中处理方式：直接返回（相似度达标时直接返回段落内容，不再走 LLM） */
    String HIT_HANDLING_DIRECTLY_RETURN = "directlyReturn";
    /** 命中处理方式：优化（交由 LLM 结合段落内容作答） */
    String HIT_HANDLING_OPTIMIZATION = "optimization";
}
