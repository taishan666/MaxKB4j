package com.tarzan.maxkb4j.core.workflow.annotation;

import com.tarzan.maxkb4j.core.workflow.enums.NodeType;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NodeHandlerType {
    /**
     * 支持的节点类型（NodeType 的 key 值）
     */
    NodeType[] value();
}