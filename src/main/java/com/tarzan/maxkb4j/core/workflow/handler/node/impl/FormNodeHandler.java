package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.formcollect.impl.FormNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class FormNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        FormNode.NodeParams nodeParams = node.getNodeData().toJavaObject(FormNode.NodeParams.class);
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
            Set<String> extractVariables = workflow.extractVariables(formContentFormat);
            Map<String, Object> variables = new HashMap<>();
            if (!extractVariables.isEmpty()) {
                for (String promptVariable : extractVariables) {
                    variables.put(promptVariable, workflow.getPromptVariables().getOrDefault(promptVariable, "*"));
                }
                variables.put("form", form);
            }
            PromptTemplate promptTemplate = PromptTemplate.from(formContentFormat);
            String answerText = promptTemplate.apply(variables).text();
            node.setAnswerText(answerText);
            node.getDetail().put("form_field_list", formFieldList);
            return new NodeResult(Map.of("is_submit", false), Map.of());
        }
    }
}
