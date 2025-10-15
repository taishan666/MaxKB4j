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
        super.setType(FORM.getKey());
        super.setViewType("single_view");
    }

    @Override
    public NodeResult execute() {
        FormNodeParams nodeParams = super.getNodeData().toJavaObject(FormNodeParams.class);
        Map<String, Object> formData = nodeParams.getFormData();
        if (formData != null) {
            Map<String, Object> nodeVariable=new HashMap<>();
            nodeVariable.put("is_submit", true);
            nodeVariable.put("form_data", formData);
            nodeVariable.putAll(formData);
            return new NodeResult(nodeVariable, Map.of());
        } else {
            List<JSONObject> formFieldList = nodeParams.getFormFieldList();
            JSONObject formSetting = new JSONObject();
            formSetting.put("form_field_list", formFieldList);
            String form = "<form_render>" + formSetting + "</form_render>";
            String formContentFormat = nodeParams.getFormContentFormat();
            Set<String> extractVariables = super.extractVariables(formContentFormat);
            Map<String, Object> variables = new HashMap<>();
            if (!extractVariables.isEmpty()) {
                for (String promptVariable : extractVariables) {
                    variables.put(promptVariable, super.getPromptVariables().getOrDefault(promptVariable, "*"));
                }
                variables.put("form", form);
            }
            PromptTemplate promptTemplate = PromptTemplate.from(formContentFormat);
            String answerText = promptTemplate.apply(variables).text();
            super.setAnswerText(answerText);
            detail.put("form_field_list", formFieldList);
            return new NodeResult(Map.of("is_submit", false), Map.of());
        }
    }

    @Override
    public void saveContext(JSONObject detail) {
        @SuppressWarnings("unchecked")
        Map<String, Object> formData = (Map<String, Object>) detail.get("form_data");
        if (formData != null){
            context.putAll(formData);
        }
        context.put("form_data", formData);
    }


}
