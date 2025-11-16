package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;

public class FormNode extends INode {

    public FormNode(JSONObject properties) {
        super(properties);
        super.setType(FORM.getKey());
        super.setViewType("single_view");
    }


    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        Map<String, Object> formData = (Map<String, Object>) detail.get("form_data");
        if (formData != null){
            context.putAll(formData);
        }
        context.put("form_data", formData);
    }


    @Data
    public static class NodeParams {
        private List<JSONObject> formFieldList;
        private String formContentFormat;
        private Map<String,Object> formData;
    }


}
