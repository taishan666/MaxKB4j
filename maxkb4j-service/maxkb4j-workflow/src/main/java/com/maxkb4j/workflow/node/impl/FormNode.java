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
    public List<Answer> getAnswerList(String chatRecordId) {
        String runtimeNodeId = (String) detail.get(RuntimeDetailField.RUNTIME_NODE_ID);
        String formRender = buildFormRender(chatRecordId,runtimeNodeId,new JSONObject(detail));
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

    /**
     * 组装以渲染标签包裹的表单设置字符串。
     */
    private String buildFormRender(String chatRecordId,String runtimeNodeId ,JSONObject nodeDetail) {
        JSONObject formSetting = new JSONObject();
        formSetting.put(FormField.FORM_FIELD_LIST, nodeDetail.getJSONArray(FormField.FORM_FIELD_LIST));
        formSetting.put(FormField.IS_SUBMIT, nodeDetail.getBooleanValue(FormField.IS_SUBMIT));
        formSetting.put(FormField.FORM_DATA, nodeDetail.getJSONObject(FormField.FORM_DATA));
        formSetting.put(RuntimeDetailField.RUNTIME_NODE_ID, runtimeNodeId);
        formSetting.put(ChatField.CHAT_RECORD_ID, chatRecordId);
        return "<" + FormField.FORM_RENDER_TAG + ">" + formSetting + "</" + FormField.FORM_RENDER_TAG + ">";
    }

    @Data
    public static class NodeParams {
        private JSONArray formFieldList;
        private String formContentFormat;
        private Map<String, Object> formData;
    }

}
