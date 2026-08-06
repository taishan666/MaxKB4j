package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.Map;

@NodeCreatorType(NodeType.FORM)
public class FormNode extends AbsNode {

    public FormNode(String id, JSONObject properties) {
        super(id, properties);
        super.setViewType("single_view");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        Map<String, Object> formData = (Map<String, Object>) detail.get("form_data");
        if (formData != null) {
            context.putAll(formData);
        }
        context.put("form_data", formData);
        context.put("is_submit", detail.get("is_submit"));
        context.put("form_field_list", detail.get("form_field_list"));
        context.put("form_content_format", detail.get("form_content_format"));
    }

    @Data
    public static class NodeParams {
        private JSONArray formFieldList;
        private String formContentFormat;
        private Map<String, Object> formData;
    }

}
