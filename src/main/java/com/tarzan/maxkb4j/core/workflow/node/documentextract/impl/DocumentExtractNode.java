package com.tarzan.maxkb4j.core.workflow.node.documentextract.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;

import java.util.List;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.DOCUMENT_EXTRACT;

public class DocumentExtractNode extends INode {


    public DocumentExtractNode(JSONObject properties) {
        super(properties);
        this.setType(DOCUMENT_EXTRACT.getKey());
    }

    String splitter = "\n-----------------------------------\n";


    @Override
    public void saveContext(JSONObject detail) {
        @SuppressWarnings("unchecked")
        List<String> content= (List<String>) detail.get("content");
        context.put("content", String.join(splitter, content));
        context.put("documentList", detail.get("documentList"));
    }


    @Override
    public JSONObject getDetail() {
        String content = (String) context.get("content");
        detail.put("content", content.split(splitter));
        return detail;
    }

}
