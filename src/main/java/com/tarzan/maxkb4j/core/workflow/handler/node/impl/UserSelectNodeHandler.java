package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.form.RadioCardFiled;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.impl.UserSelectNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.USER_SELECT)
@Component
public class UserSelectNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        UserSelectNode.NodeParams nodeParams = node.getNodeData().toJavaObject(UserSelectNode.NodeParams.class);
        JSONObject formData = nodeParams.getFormData();
        Map<String, Object> options = new HashMap<>();
        List<UserSelectNode.Branch> branches = nodeParams.getBranch();
        for (UserSelectNode.Branch branch : branches) {
            options.put(branch.getOption(), branch.getId());
        }
        Map<String, Object> nodeVariable = new HashMap<>();
        String SELECT_FILED = "select-card";
        if (formData != null) {
            nodeVariable.put("is_submit", true);
            nodeVariable.put("form_data", formData);
            String branchId = formData.getString(SELECT_FILED);
            nodeVariable.put("branchId", branchId);
            UserSelectNode.Branch selectBranch = branches.stream().filter(branch -> branch.getId().equals(branchId)).findFirst().orElse(null);
            nodeVariable.put("branchName", selectBranch == null ? "" : selectBranch.getOption());
        } else {
            String labelName = workflow.generatePrompt(nodeParams.getLabelName());
            RadioCardFiled radioCardFiled = new RadioCardFiled(labelName, SELECT_FILED, options);
            List<RadioCardFiled> formFieldList = List.of(radioCardFiled);
            JSONObject formSetting = new JSONObject();
            formSetting.put("form_field_list", formFieldList);
            String formRender = "<card_selection_render>" + formSetting + "</card_selection_render>";
            node.setAnswerText(formRender);
            nodeVariable.put("is_submit", false);

        }
        return new NodeResult(nodeVariable, false, this::isInterrupt);
    }

    public boolean isInterrupt(AbsNode node) {
        return !(boolean) node.getContext().getOrDefault("is_submit", false);
    }
}
