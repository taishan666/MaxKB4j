package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.FormNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.ChatField;
import static com.maxkb4j.workflow.consts.WorkflowConstants.FormField;

@NodeHandlerType(NodeType.FORM)
@Component
public class FormNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        FormNode.NodeParams params = parseParams(node, FormNode.NodeParams.class);
        Map<String, Object> formData = params.getFormData();
        Map<String, Object> nodeVariable = new HashMap<>();
        if (formData != null) {
            nodeVariable.put(FormField.IS_SUBMIT, true);
            nodeVariable.put(FormField.FORM_DATA, new JSONObject(formData));
            nodeVariable.putAll(formData);
        } else {
            JSONArray formFieldList = params.getFormFieldList();
            JSONObject formSetting = new JSONObject();
            formSetting.put(FormField.FORM_FIELD_LIST, formFieldList);
            String formRender = "<" + FormField.FORM_RENDER_TAG + ">" + formSetting + "</" + FormField.FORM_RENDER_TAG + ">";
            String formContentFormat = params.getFormContentFormat();
            String answerText = workflow.renderPrompt(formContentFormat, Map.of("form", formRender));
            setAnswerText(node, answerText);
            nodeVariable.put(FormField.FORM_FIELD_LIST, formFieldList);
            nodeVariable.put(FormField.FORM_CONTENT_FORMAT, formContentFormat);
            nodeVariable.put(FormField.IS_SUBMIT, false);
        }
        if (workflow instanceof IChatWorkflow chatWorkflow){
            String chatRecordId=chatWorkflow.getChatParams().getChatId();
            putDetail(node, ChatField.CHAT_RECORD_ID, chatRecordId);
        }
        return new NodeResult(nodeVariable, false, this::shouldInterrupt);
    }

    @Override
    public boolean shouldInterrupt(AbsNode node) {
        return !(boolean) node.getContext().getOrDefault(FormField.IS_SUBMIT, false);
    }
}
