package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.FormNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@NodeHandlerType(NodeType.FORM)
@Component
public class FormNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        FormNode.NodeParams nodeParams = node.getNodeData().toJavaObject(FormNode.NodeParams.class);
        Map<String, Object> formData = nodeParams.getFormData();
        Map<String, Object> nodeVariable=new HashMap<>();
        if (formData != null) {
            nodeVariable.put("is_submit", true);
            nodeVariable.put("form_data", formData);
            nodeVariable.putAll(formData);
            node.getDetail().put("form_data", formData);
        } else {
            JSONArray formFieldList = nodeParams.getFormFieldList();
            JSONObject formSetting = new JSONObject();
            formSetting.put("form_field_list", formFieldList);
            String form = "<form_render>" + formSetting + "</form_render>";
            String formContentFormat = nodeParams.getFormContentFormat();
            String answerText =workflow.getTemplateRenderer().render(formContentFormat,Map.of("form", form));
            node.setAnswerText(answerText);
            nodeVariable.put("form_field_list", formFieldList);
            nodeVariable.put("form_content_format", formContentFormat);
            nodeVariable.put("is_submit", false);
        }
        return new NodeResult(nodeVariable,false,this::isInterrupt);
    }



    public boolean isInterrupt(AbsNode node) {
        return !(boolean)node.getContext().getOrDefault("is_submit", false);
    }

}
