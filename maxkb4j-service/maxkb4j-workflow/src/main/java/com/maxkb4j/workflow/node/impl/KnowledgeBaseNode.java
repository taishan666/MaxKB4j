package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.node.AbsNode;

import static com.maxkb4j.workflow.enums.NodeType.KNOWLEDGE_BASE;

@NodeCreatorType(NodeType.KNOWLEDGE_BASE)
public class KnowledgeBaseNode extends AbsNode {
    public KnowledgeBaseNode(String id, JSONObject properties) {
        super(id, properties);
        super.setType(KNOWLEDGE_BASE.getKey());
    }

}
