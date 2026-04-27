package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.form.RadioCardField;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.UserSelectNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.USER_SELECT)
@Component
public class UserSelectNodeHandler extends AbsNodeHandler {

    private static final String SELECT_FILED = "select-card";
    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        UserSelectNode.NodeParams params = parseParams(node, UserSelectNode.NodeParams.class);
        JSONObject formData = params.getFormData();
        List<UserSelectNode.Branch> branches = params.getBranch();
        Map<String, Object> nodeVariable = new HashMap<>();
        if (formData != null) {
            nodeVariable.put("is_submit", true);
            nodeVariable.put("form_data", formData);
            String branchId = formData.getString(SELECT_FILED);
            nodeVariable.put("branchId", branchId);
            UserSelectNode.Branch selectBranch = branches.stream()
                    .filter(branch -> branch.getId().equals(branchId))
                    .findFirst()
                    .orElse(null);
            nodeVariable.put("branchName", selectBranch == null ? "" : selectBranch.getOption());
        } else {
            Map<String, Object> options = new LinkedHashMap<>();
            for (UserSelectNode.Branch branch : branches) {
                options.put(branch.getOption(), branch.getId());
            }
            String labelName = workflow.renderPrompt(params.getLabelName());
            RadioCardField radioCardFiled = new RadioCardField(labelName, SELECT_FILED, options);
            List<RadioCardField> formFieldList = List.of(radioCardFiled);
            JSONObject formSetting = new JSONObject();
            formSetting.put("form_field_list", formFieldList);
            String formRender = "<card_selection_render>" + formSetting + "</card_selection_render>";
            setAnswer(node, formRender);
            nodeVariable.put("form_field_list", formFieldList);
            nodeVariable.put("is_submit", false);
        }

        return new NodeResult(nodeVariable, false, this::shouldInterrupt);
    }

    @Override
    public boolean shouldInterrupt(AbsNode node) {
        return !(boolean) node.getContext().getOrDefault("is_submit", false);
    }
}
