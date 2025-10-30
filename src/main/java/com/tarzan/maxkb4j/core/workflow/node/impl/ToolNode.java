package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TOOL;

public class ToolNode extends INode {
    public ToolNode(JSONObject properties) {
        super(properties);
        this.setType(TOOL.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("result", detail.get("result"));
    }

    @Data
    public static class NodeParams {
        private List<ToolInputField> inputFieldList;
        private String code;
    }
}
