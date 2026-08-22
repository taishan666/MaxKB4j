package com.maxkb4j.workflow.node.impl;

import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.FORM)
public class FormNode extends AbsNode {

    public FormNode(String id, JSONObject properties) {
        super(id, properties);
        super.setViewType(ViewType.SINGLE_VIEW);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        Map<String, Object> formData = (Map<String, Object>) detail.get(FormField.FORM_DATA);
        if (formData != null) {
            context.putAll(formData);
        }
        context.put(FormField.FORM_DATA, formData);
        context.put(FormField.IS_SUBMIT, detail.get(FormField.IS_SUBMIT));
        context.put(FormField.FORM_FIELD_LIST, detail.get(FormField.FORM_FIELD_LIST));
        context.put(FormField.FORM_CONTENT_FORMAT, detail.get(FormField.FORM_CONTENT_FORMAT));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Answer> getAnswerList(String chatRecordId) {
        Map<String, Object> formData = (Map<String, Object>) context.getOrDefault(FormField.FORM_DATA, Map.of());
        boolean isSubmit = (boolean) context.getOrDefault(FormField.IS_SUBMIT, false);
        String runtimeNodeId = this.getRuntimeNodeId();
        JSONArray formFieldList = new JSONArray();
        if (!formData.isEmpty()) {
            formFieldList = (JSONArray) context.getOrDefault(FormField.FORM_FIELD_LIST, new JSONArray());
        }
        JSONObject formSetting = new JSONObject();
        formSetting.put(FormField.FORM_FIELD_LIST, formFieldList);
        formSetting.put(FormField.IS_SUBMIT, isSubmit);
        formSetting.put(FormField.FORM_DATA, formData);
        formSetting.put(RuntimeDetailField.RUNTIME_NODE_ID, runtimeNodeId);
        formSetting.put(ChatField.CHAT_RECORD_ID, chatRecordId);
        String formRender = "<" + FormField.FORM_RENDER_TAG + ">" + formSetting + "</" + FormField.FORM_RENDER_TAG + ">";
        JSONObject nodeData = this.getNodeData();
        if (nodeData != null) {
            String formContentFormat = nodeData.getString(FormField.FORM_CONTENT_FORMAT);
            if (formContentFormat != null) {
                PromptTemplate promptTemplate = PromptTemplate.from(formContentFormat);
                String answer = promptTemplate.apply(Map.of("form", formRender)).text();
                return List.of(Answer.builder().content(answer).reasoningContent("").chatRecordId(chatRecordId).runtimeNodeId(runtimeNodeId).realNodeId(runtimeNodeId).viewType(this.getViewType()).build());
            }
        }
        return List.of();
    }

    @Data
    public static class NodeParams {
        private JSONArray formFieldList;
        private String formContentFormat;
        private Map<String, Object> formData;
    }

}
