package com.tarzan.maxkb4j.core.workflow.node.userselect.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.userselect.input.UserSelectBranch;
import com.tarzan.maxkb4j.core.workflow.node.userselect.input.UserSelectNodeParams;

import java.util.HashMap;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

public class UserSelectNode extends INode {


    @Override
    public NodeResult execute() {
        System.out.println(USER_SELECT);
        UserSelectNodeParams nodeParams = super.nodeParams.toJavaObject(UserSelectNodeParams.class);
        JSONObject formData = nodeParams.getFormData();
        Map<String, String> selectMap = new HashMap<>();
        for (UserSelectBranch branch : nodeParams.getBranch()) {
            selectMap.put(branch.getId(), branch.getCondition());
        }
        if (formData != null) {
            context.put("is_submit", true);
            context.put("form_data", formData);
            for (String key : formData.keySet()) {
                context.put(key, formData.get(key));
            }
        } else {
            context.put("is_submit", false);
        }

        String branchId=nodeParams.getBranch().get(0).getId();
        return new NodeResult(Map.of("branch_id", branchId, "branch_name", selectMap.get(branchId)), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer",context.get("answer"));
        detail.put("branch_id",context.get("branch_id"));
        detail.put("branch_name",context.get("branch_name"));
        return detail;
    }
}
