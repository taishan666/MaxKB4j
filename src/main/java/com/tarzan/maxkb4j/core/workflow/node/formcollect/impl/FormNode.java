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
        this.type = FORM.getKey();
    }

    @Override
    public NodeResult execute() {
        FormNodeParams nodeParams=super.nodeParams.toJavaObject(FormNodeParams.class);
        List<JSONObject> formFieldList = nodeParams.getFormFieldList();
        JSONObject formData = nodeParams.getFormData();
        if (formData != null) {
            context.put("is_submit", true);
            context.put("form_data", formData);
            for (String key : formData.keySet()) {
                context.put(key, formData.get(key));
            }
            return new NodeResult(Map.of(), Map.of());
        } else {
            context.put("is_submit", false);
            JSONObject formSetting = new JSONObject();
            formSetting.put("form_field_list", formFieldList);
            formSetting.put("runtimeNodeId", super.getRuntimeNodeId());
            formSetting.put("chatRecordId", super.getFlowParams().getChatRecordId());
            formSetting.put("is_submit", context.getOrDefault("is_submit", false));
            String form = "<form_render>" + formSetting + "</form_render>";
            // Get workflow content and reset prompt todo (如果表单里有变量)
         //   String updatedFormContentFormat = workflowManage.resetPrompt(formContentFormat);
            String formContentFormat = nodeParams.getFormContentFormat();
            PromptTemplate promptTemplate = PromptTemplate.from(formContentFormat);
            String formRender = promptTemplate.apply(Map.of("form", form)).text();
            return new NodeResult(Map.of("result", formRender, "answer", formRender,
                    "form_field_list", formFieldList,
                    "form_content_format", formContentFormat), Map.of());
        }
    }

    @Override
    public List<Answer> getAnswerList() {
        String formContentFormat = (String) context.get("form_content_format");
        JSONObject formSetting = new JSONObject();
        formSetting.put("form_field_list", context.get("form_field_list"));
        formSetting.put("runtimeNodeId", super.getRuntimeNodeId());
        formSetting.put("chatRecordId", super.getFlowParams().getChatRecordId());
        formSetting.put("is_submit", context.getOrDefault("is_submit", false));
        String form = "<form_render>" + formSetting + "</form_render>";
        String updatedFormContentFormat = workflowManage.resetPrompt(formContentFormat);
        PromptTemplate promptTemplate = PromptTemplate.from(updatedFormContentFormat);
        String value = promptTemplate.apply(Map.of("form", form)).text();
        Answer answer = new Answer(value, this.getViewType(), this.runtimeNodeId,
                this.getFlowParams().getChatRecordId(), new HashMap<>());
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
