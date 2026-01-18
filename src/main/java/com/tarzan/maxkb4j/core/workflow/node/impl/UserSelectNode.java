package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

public class UserSelectNode extends AbsNode {
    public UserSelectNode(String id,JSONObject properties) {
        super(id,properties);
        super.setType(USER_SELECT.getKey());
        super.setViewType("single_view");
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("branchName", detail.get("branchName"));
    }

    @Data
    public static class NodeParams {
        private List<Branch> branch;
        private JSONObject formData;
        private String labelName;
        private Boolean isResult;
    }

    @Data
    public static class Branch {
        private String id;
        private String type;
        private String option;
    }

}
