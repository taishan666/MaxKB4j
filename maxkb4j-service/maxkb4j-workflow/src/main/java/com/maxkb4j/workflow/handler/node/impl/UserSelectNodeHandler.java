package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.form.RadioCardField;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.UserSelectNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType(NodeType.USER_SELECT)
@Component
public class UserSelectNodeHandler extends AbsNodeHandler {

    private static final String SELECT_FILED = "select-card";
    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        UserSelectNode.NodeParams params = parseParams(node, UserSelectNode.NodeParams.class);
        JSONObject formData = params.getFormData();
        List<UserSelectNode.Branch> branches = params.getBranch();
        Map<String, Object> nodeVariable = new HashMap<>();
        if (formData != null) {
            nodeVariable.put(FormField.IS_SUBMIT, true);
            nodeVariable.put(FormField.FORM_DATA, formData);
            String branchId = formData.getString(SELECT_FILED);
            nodeVariable.put(NodeField.BRANCH_ID, branchId);
            UserSelectNode.Branch selectBranch = branches.stream()
                    .filter(branch -> branch.getId().equals(branchId))
                    .findFirst()
                    .orElse(null);
            nodeVariable.put(NodeField.BRANCH_NAME, selectBranch == null ? "" : selectBranch.getOption());
        } else {
            Map<String, Object> options = new LinkedHashMap<>();
            for (UserSelectNode.Branch branch : branches) {
                options.put(branch.getOption(), branch.getId());
            }
            String labelName = workflow.renderPrompt(params.getLabelName());
            RadioCardField radioCardFiled = new RadioCardField(labelName, SELECT_FILED, options);
            List<RadioCardField> formFieldList = List.of(radioCardFiled);
            JSONObject formSetting = new JSONObject();
            formSetting.put(FormField.FORM_FIELD_LIST, formFieldList);
            String formRender = "<" + FormField.CARD_SELECTION_RENDER_TAG + ">" + formSetting + "</" + FormField.CARD_SELECTION_RENDER_TAG + ">";
            setAnswerText(node, formRender);
            nodeVariable.put(FormField.FORM_FIELD_LIST, formFieldList);
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
