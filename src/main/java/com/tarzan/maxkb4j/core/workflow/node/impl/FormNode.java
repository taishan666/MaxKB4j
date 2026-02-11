package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Answer;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;

public class FormNode extends AbsNode {

    public FormNode(String id, JSONObject properties) {
        super(id, properties);
        super.setType(FORM.getKey());
        super.setViewType("single_view");
    }


    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
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
    public List<Answer> getAnswerList()  {
        Map<String, Object> formData = (Map<String, Object>) context.getOrDefault("form_data",Map.of());
        boolean isSubmit = (boolean) context.getOrDefault("is_submit",false);
        String runtimeNodeId=this.getRuntimeNodeId();
        JSONArray formFieldList =(JSONArray)context.getOrDefault("form_field_list", new JSONArray());
        String formContentFormat = (String) context.getOrDefault("form_content_format","");
        JSONObject formSetting = new JSONObject();
        formSetting.put("form_field_list", formFieldList);
        formSetting.put("is_submit", isSubmit);
        formSetting.put("form_data", formData);
        formSetting.put("runtimeNodeId", runtimeNodeId);
        String formRender = "<form_render>" + formSetting + "</form_render>";
        String answerText = super.getTemplateRenderer().render(formContentFormat,Map.of("form", formRender));
        return List.of(Answer.builder().content(answerText).reasoningContent("").runtimeNodeId(runtimeNodeId).viewType(this.getViewType()).build());
    }


    @Data
    public static class NodeParams {
        private JSONArray formFieldList;
        private String formContentFormat;
        private Map<String, Object> formData;
    }


}
