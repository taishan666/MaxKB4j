package com.tarzan.maxkb4j.core.workflow.node.formcollect.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.formcollect.input.FormNodeParams;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;

public class FormNode extends INode {

    public FormNode(JSONObject properties) {
        super(properties);
        this.type = FORM.getKey();
        this.viewType = "single_view";
    }

    @Override
    public NodeResult execute() {
        FormNodeParams nodeParams = super.getNodeData().toJavaObject(FormNodeParams.class);
        Map<String, Object> formData = nodeParams.getFormData();
        if (formData != null) {
            context.put("is_submit", true);
            context.put("form_data", formData);
            context.putAll(formData);
            return new NodeResult(Map.of(), Map.of());
        } else {
            List<JSONObject> formFieldList = nodeParams.getFormFieldList();
            context.put("is_submit", false);
            JSONObject formSetting = new JSONObject();
            formSetting.put("form_field_list", formFieldList);
            formSetting.put("is_submit", false);
            formSetting.put("runtimeNodeId", "123456789");
           // formSetting.put("chatRecordId", "6666666666");
            String form = "<form_render>" + formSetting + "</form_render>";
            String formContentFormat = nodeParams.getFormContentFormat();
            Set<String> extractVariables = super.extractVariables(formContentFormat);
            Map<String, Object> variables = new HashMap<>();
            if (!extractVariables.isEmpty()) {
                for (String promptVariable : extractVariables) {
                    variables.put(promptVariable, promptVariables.getOrDefault(promptVariable, "*"));
                }
                variables.put("form", form);
            }
            PromptTemplate promptTemplate = PromptTemplate.from(formContentFormat);
            String formRender = promptTemplate.apply(variables).text();
            return new NodeResult(Map.of("answer", formRender,
                    "form_field_list", formFieldList), Map.of());
        }
    }

    @Override
    public void saveContext(JSONObject detail) {
        context.put("form_field_list", detail.get("form_field_list"));
        @SuppressWarnings("unchecked")
        Map<String, Object> formData = (Map<String, Object>) detail.get("form_data");
        if (formData != null){
            context.putAll(formData);
        }
        context.put("form_data", formData);
        context.put("is_submit", detail.get("is_submit"));
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("form_field_list", context.get("form_field_list"));
        detail.put("form_data", context.get("form_data"));
        detail.put("is_submit", context.get("is_submit"));
        return detail;
    }

}
