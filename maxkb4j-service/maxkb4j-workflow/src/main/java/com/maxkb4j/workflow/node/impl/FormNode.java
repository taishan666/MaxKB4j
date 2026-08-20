package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
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

    @Override
    @SuppressWarnings("unchecked")
    public List<Answer> getAnswerList(String chatRecordId)  {
        Map<String, Object> formData = (Map<String, Object>) context.getOrDefault("form_data",Map.of());
        boolean isSubmit = (boolean) context.getOrDefault("is_submit",false);
        String runtimeNodeId=this.getRuntimeNodeId();
        JSONArray formFieldList =new JSONArray();
        if (!formData.isEmpty()){
            formFieldList =(JSONArray)context.getOrDefault("form_field_list", new JSONArray());
        }
        JSONObject formSetting = new JSONObject();
        formSetting.put("form_field_list", formFieldList);
        formSetting.put("is_submit", isSubmit);
        formSetting.put("form_data", formData);
        formSetting.put("runtimeNodeId", runtimeNodeId);
        formSetting.put("chatRecordId", chatRecordId);
        String formRender = "<form_render>" + formSetting + "</form_render>";
        return List.of(Answer.builder().content(formRender).reasoningContent("").chatRecordId(chatRecordId).runtimeNodeId(runtimeNodeId).realNodeId(runtimeNodeId).viewType(this.getViewType()).build());
    }

    @Data
    public static class NodeParams {
        private JSONArray formFieldList;
        private String formContentFormat;
        private Map<String, Object> formData;
    }

}
