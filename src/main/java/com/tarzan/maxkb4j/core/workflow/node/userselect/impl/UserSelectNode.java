package com.tarzan.maxkb4j.core.workflow.node.userselect.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.Data;

import java.util.List;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

public class UserSelectNode extends INode {
    public UserSelectNode(JSONObject properties) {
        super(properties);
        super.setType(USER_SELECT.getKey());
        super.setViewType("single_view");
    }

    @Override
    protected void saveContext(Workflow workflow, JSONObject detail) {

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
