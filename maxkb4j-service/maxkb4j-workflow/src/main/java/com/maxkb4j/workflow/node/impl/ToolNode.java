package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.entity.ToolInputField;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

@NodeCreatorType(NodeType.TOOL)
public class ToolNode extends AbsNode {
    public ToolNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("result", detail.get("result"));
    }

    @Data
    public static class NodeParams {
        private String toolLibId;
        private List<ToolInputField> inputFieldList;
        private String code;
        private Map<String,Object> initParams;
        private Boolean isResult;
        private String toolType;
    }
}
