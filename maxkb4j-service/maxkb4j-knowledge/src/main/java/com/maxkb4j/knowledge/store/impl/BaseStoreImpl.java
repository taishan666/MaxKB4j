package com.maxkb4j.knowledge.store.impl;

import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.store.IDataStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 检索/写入 store 的抽象基类，仅承担通用的前置校验。
 * <p>检索编排策略（排除非激活段落、问题到段落的映射、去重排序）已上移到
 * {@link com.maxkb4j.knowledge.retriever.SearchOrchestrator}，store 不再依赖任何 service，
 * 原构造期循环依赖随之消除。</p>
 */
@Slf4j
public abstract class BaseStoreImpl implements IDataStore {

    /**
     * 搜索前置校验：knowledgeIds 或 query 为空时短路
     */
    protected boolean shouldShortCircuit(SearchRequest request) {
        if (request == null) {
            return true;
        }
        if (request.getKnowledgeIds() == null || request.getKnowledgeIds().isEmpty()) {
            return true;
        }
        return StringUtils.isBlank(request.getQuery());
    }
}