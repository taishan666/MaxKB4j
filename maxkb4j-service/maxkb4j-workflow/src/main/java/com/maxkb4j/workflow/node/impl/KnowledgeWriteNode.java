package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.KNOWLEDGE_WRITE)
public class KnowledgeWriteNode extends AbsNode {

    public KnowledgeWriteNode(String id, JSONObject properties) {
        super(id, properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(NodeField.SQL, detail.get(NodeField.SQL));
        context.put(NodeField.RESULT, detail.get(NodeField.RESULT));
    }

    @Data
    public static class NodeParams  {
        private List<String> documentList;
    }
}
