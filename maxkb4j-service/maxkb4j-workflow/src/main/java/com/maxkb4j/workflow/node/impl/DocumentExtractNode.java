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

@NodeCreatorType(NodeType.DOCUMENT_EXTRACT)
public class DocumentExtractNode extends AbsNode {

    public DocumentExtractNode(String id, JSONObject properties) {
        super(id, properties);
    }

    public static final String SPLITTER = "\n-----------------------------------\n";

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        String[] content = (String[]) detail.get(NodeField.CONTENT);
        context.put(NodeField.CONTENT, String.join(SPLITTER, content));
        context.put(NodeField.DOCUMENT_LIST, detail.get(NodeField.DOCUMENT_LIST));
    }

    @Override
    public Map<String, Object> getDetail() {
        String content = (String) context.getOrDefault(NodeField.CONTENT, "");
        detail.put(NodeField.CONTENT, content.split(SPLITTER));
        detail.put(NodeField.DOCUMENT_LIST, context.get(NodeField.DOCUMENT_LIST));
        return detail;
    }

    @Data
    public static class NodeParams {
        private List<String> documentList;
    }

}
