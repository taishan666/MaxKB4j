package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.FormNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FormNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        FormNode.NodeParams nodeParams = node.getNodeData().toJavaObject(FormNode.NodeParams.class);
        Map<String, Object> formData = nodeParams.getFormData();
        Map<String, Object> nodeVariable=new HashMap<>();
        if (formData != null) {
            nodeVariable.put("is_submit", true);
            nodeVariable.put("form_data", formData);
            nodeVariable.putAll(formData);
            node.getDetail().put("form_data", formData);
        } else {
            List<JSONObject> formFieldList = nodeParams.getFormFieldList();
            JSONObject formSetting = new JSONObject();
            formSetting.put("form_field_list", formFieldList);
            String form = "<form_render>" + formSetting + "</form_render>";
            String formContentFormat = nodeParams.getFormContentFormat();
            String answerText =workflow.generatePrompt(formContentFormat,Map.of("form", form));
            node.setAnswerText(answerText);
            node.getDetail().put("form_field_list", formFieldList);
            nodeVariable.put("is_submit", false);
        }
        return new NodeResult(nodeVariable, Map.of(),this::writeContext,this::isInterrupt);
    }



    public void writeContext(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, Workflow workflow) {
        node.getContext().putAll(nodeVariable);
        node.getDetail().put("form_data",nodeVariable.get("form_data"));
        if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable))&& StringUtils.isNotBlank(node.getAnswerText())) {
            ChatMessageVO vo = node.toChatMessageVO(
                    workflow.getChatParams().getChatId(),
                    workflow.getChatParams().getChatRecordId(),
                    node.getAnswerText(),
                    "",
                    false);
            workflow.getChatParams().getSink().tryEmitNext(vo);
            workflow.setAnswer(workflow.getAnswer()+node.getAnswerText());
            ChatMessageVO endVo = node.toChatMessageVO(
                    workflow.getChatParams().getChatId(),
                    workflow.getChatParams().getChatRecordId(),
                    "",
                    "",
                    true);
            workflow.getChatParams().getSink().tryEmitNext(endVo);
        }
    }


    public boolean isInterrupt(INode node) {
        return !(boolean)node.getContext().getOrDefault("is_submit", false);
    }

}
