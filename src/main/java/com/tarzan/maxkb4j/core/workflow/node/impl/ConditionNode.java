package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.CONDITION;

public class ConditionNode extends INode {

    public ConditionNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(CONDITION.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("branchName", detail.get("branchName"));
    }

    @Data
    public static class NodeParams {
        private List<Branch> branch;
    }

    @Data
    public static class Branch {
        private String id;
        private String type;
        private String condition;
        private List<Condition> conditions;
    }


    @Data
    public static class Condition {
        private List<String> field;

        @NotBlank(message = "Compare operation cannot be blank")
        private String compare;

        @NotBlank(message = "Value cannot be blank")
        private String value;
    }




}
