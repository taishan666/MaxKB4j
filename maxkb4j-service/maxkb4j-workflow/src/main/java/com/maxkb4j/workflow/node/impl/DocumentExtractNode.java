package com.maxkb4j.workflow.node.impl;

import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.util.ContextValueUtil;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.DOCUMENT_EXTRACT)
public class DocumentExtractNode extends AbsNode {

    public static final String SPLITTER = "\n-----------------------------------\n";

    public DocumentExtractNode(String id, JSONObject properties) {
        super(id, properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        // content 形态随来源而异：正常执行为已拼接的 String，恢复执行（经 JSON 往返）为 JSONArray，
        // 旧数据可能为 String[]，统一转回拼接文本，不能强转 String[]
        context.put(NodeField.CONTENT, joinContent(detail.get(NodeField.CONTENT)));
        context.put(NodeField.DOCUMENT_LIST, detail.get(NodeField.DOCUMENT_LIST));
    }

    private String joinContent(Object content) {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof String[] array) {
            return String.join(SPLITTER, array);
        }
        return String.join(SPLITTER, ContextValueUtil.toList(content, String.class));
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
