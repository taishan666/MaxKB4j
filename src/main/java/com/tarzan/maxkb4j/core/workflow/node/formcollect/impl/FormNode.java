package com.tarzan.maxkb4j.core.workflow.node.formcollect.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.dto.Answer;
import com.tarzan.maxkb4j.core.workflow.node.formcollect.input.FormNodeParams;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;

public class FormNode extends INode {

    public FormNode() {
        super();
        this.type = FORM.getKey();
    }

    @Override
    public NodeResult execute() {
        FormNodeParams nodeParams=super.nodeParams.toJavaObject(FormNodeParams.class);
        List<JSONObject> formFieldList = nodeParams.getFormFieldList();
        JSONObject formData = nodeParams.getFormData();
        String formContentFormat = nodeParams.getFormContentFormat();
        if (formData != null) {
            context.put("is_submit", true);
            context.put("form_data", formData);
            for (String key : formData.keySet()) {
                context.put(key, formData.get(key));
            }
        } else {
            context.put("is_submit", false);
        }
        // Create form_setting map
        JSONObject formSetting = new JSONObject();
        formSetting.put("form_field_list", formFieldList);
        formSetting.put("runtimeNodeId", super.getRuntimeNodeId());
        formSetting.put("chatRecordId", super.getWorkflowParams().getChatRecordId());
        formSetting.put("is_submit", context.getOrDefault("is_submit", false));
        String form = "<form_rander>" + formSetting + "</form_rander>";
        // Get workflow content and reset prompt todo
     //   Map<String, Object> contextContent = workflowManage.getWorkflowContent();
        String updatedFormContentFormat = workflowManage.resetPrompt(formContentFormat);
        PromptTemplate promptTemplate = PromptTemplate.from(updatedFormContentFormat);
        String value = promptTemplate.apply(Map.of("form", form)).text();
        // Format the prompt template
        return new NodeResult(Map.of("result", value, "answer", value,
                "form_field_list", formFieldList,
                "form_content_format", formContentFormat), Map.of());
    }

    @Override
    public List<Answer> getAnswerList() {
        String formContentFormat = (String) context.get("form_content_format");
        JSONObject formSetting = new JSONObject();
        formSetting.put("form_field_list", context.get("form_field_list"));
        formSetting.put("runtimeNodeId", super.getRuntimeNodeId());
        formSetting.put("chatRecordId", super.getWorkflowParams().getChatRecordId());
        formSetting.put("is_submit", context.getOrDefault("is_submit", false));
        String form = "<form_rander>" + formSetting + "</form_rander>";
        String updatedFormContentFormat = workflowManage.resetPrompt(formContentFormat);
        PromptTemplate promptTemplate = PromptTemplate.from(updatedFormContentFormat);
        String value = promptTemplate.apply(Map.of("form", form)).text();
        Answer answer = new Answer(value, this.getViewType(), this.runtimeNodeId,
                this.workflowParams.getChatRecordId(), new HashMap<>());
        return Collections.singletonList(answer);
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("result", context.get("result"));
        detail.put("form_field_list", context.get("form_field_list"));
        detail.put("form_content_format", context.get("form_content_format"));
        detail.put("form_data", context.get("form_data"));
        detail.put("is_submit", context.get("is_submit"));
        return detail;
    }

}
