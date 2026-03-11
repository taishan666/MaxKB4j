package com.maxkb4j.workflow.annotation;


import com.maxkb4j.workflow.enums.NodeType;

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