package com.tarzan.maxkb4j.core.workflow.node.userselect.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.tarzan.maxkb4j.core.form.RadioCardFiled;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.userselect.input.UserSelectBranch;
import com.tarzan.maxkb4j.core.workflow.node.userselect.input.UserSelectNodeParams;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

public class UserSelectNode extends INode {

    private final String selectFiled;

    public UserSelectNode() {
        this.selectFiled = "select-card";
    }

    @Override
    public NodeResult execute() {
        System.out.println(USER_SELECT);
        UserSelectNodeParams nodeParams = super.nodeParams.toJavaObject(UserSelectNodeParams.class);
        JSONObject formData = nodeParams.getFormData();
        Map<String, Object> options = new HashMap<>();
        for (UserSelectBranch branch : nodeParams.getBranch()) {
            options.put(branch.getOption(), branch.getId());
        }
        if (formData != null) {
            context.put("is_submit", true);
            context.put("form_data", formData);
            String choice = formData.getString(selectFiled);
            context.put(selectFiled, choice);
            String branchId = (String) options.get(choice);
            return new NodeResult(Map.of("branch_id", branchId, "branch_name", choice), Map.of());
        } else {
            context.put("is_submit", false);
            RadioCardFiled radioCardFiled = new RadioCardFiled(nodeParams.getLabelName(), selectFiled, options);
            List<RadioCardFiled> formFieldList = List.of(radioCardFiled);
            JSONObject formSetting = new JSONObject();
            formSetting.put("form_field_list", formFieldList);
            formSetting.put("runtimeNodeId", super.getRuntimeNodeId());
            formSetting.put("chatRecordId", super.getWorkflowParams().getChatRecordId());
            formSetting.put("is_submit", context.getOrDefault("is_submit", false));
            String form = "<form_render>" + formSetting + "</form_render>";
            String formContentFormat = "{{form}} \n 填写后请点击【提交】按钮进行提交。";
            PromptTemplate promptTemplate = PromptTemplate.from(formContentFormat);
            String formRender = promptTemplate.apply(Map.of("form", form)).text();
            return new NodeResult(Map.of("result", formRender, "answer", formRender,
                    "form_field_list", formFieldList,
                    "form_content_format", formContentFormat), Map.of());
        }


    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer", context.get("answer"));
        detail.put("branch_id", context.get("branch_id"));
        detail.put("branch_name", context.get("branch_name"));
        return detail;
    }
}
