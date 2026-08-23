package com.maxkb4j.workflow.node.impl;

import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.util.FormRenderUtil;
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
      /*  context.put(FormField.IS_SUBMIT, detail.get(FormField.IS_SUBMIT));
        context.put(FormField.FORM_FIELD_LIST, detail.get(FormField.FORM_FIELD_LIST));
        context.put(FormField.FORM_CONTENT_FORMAT, detail.get(FormField.FORM_CONTENT_FORMAT));*/
    }

    @Override
    public List<Answer> getAnswerList() {
        String runtimeNodeId = (String) detail.get(RuntimeDetailField.RUNTIME_NODE_ID);
        String formRender = FormRenderUtil.buildFormRender(runtimeNodeId, new JSONObject(detail), FormField.FORM_RENDER_TAG);
        JSONObject nodeData = this.getNodeData();
        if (nodeData != null) {
            String formContentFormat = nodeData.getString(FormField.FORM_CONTENT_FORMAT);
            if (formContentFormat != null) {
                PromptTemplate promptTemplate = PromptTemplate.from(formContentFormat);
                String answer = promptTemplate.apply(Map.of("form", formRender)).text();
                return List.of(Answer.builder().content(answer).reasoningContent("").chatRecordId("").runtimeNodeId(runtimeNodeId).realNodeId(runtimeNodeId).viewType(this.getViewType()).build());
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
