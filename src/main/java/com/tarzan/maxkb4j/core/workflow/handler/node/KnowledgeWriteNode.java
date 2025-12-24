package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.KNOWLEDGE_WRITE;

public class KnowledgeWriteNode extends INode {

    public KnowledgeWriteNode(String id, JSONObject properties) {
        super(id, properties);
        this.setType(KNOWLEDGE_WRITE.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("sql", detail.get("sql"));
        context.put("result", detail.get("result"));
    }

    @Data
    public static class NodeParams  {
        private List<String> documentList;
    }
}
