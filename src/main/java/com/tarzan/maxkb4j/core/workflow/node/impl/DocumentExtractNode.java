package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.DOCUMENT_EXTRACT;

public class DocumentExtractNode extends INode {


    public DocumentExtractNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(DOCUMENT_EXTRACT.getKey());
    }

    String splitter = "\n-----------------------------------\n";


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        @SuppressWarnings("unchecked")
        List<String> content= (List<String>) detail.get("content");
        context.put("content", String.join(splitter, content));
        Object documentList=detail.get("documentList");
        context.put("documentList", JSON.toJSON(documentList));
    }


    @Override
    public Map<String, Object> getDetail() {
        String content = (String) context.getOrDefault("content","");
        detail.put("content", content.split(splitter));
        return detail;
    }


    @Data
    public static class NodeParams  {
        private List<String> documentList;
    }


}
